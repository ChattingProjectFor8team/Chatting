package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.entity.UserJellyBalance;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.repository.UserJellyBalanceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("젤리 동시 차감 — 3종 락 비교")
class JellyConcurrencyTest {

    // ── 의존성 주입 ──

    @Autowired JellyService jellyService;
    @Autowired UserJellyBalanceRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TransactionTemplate transactionTemplate;

    // ── 공통 상수 ──

    private static final int INITIAL_BALANCE = 10_000;
    private static final int DEDUCT_AMOUNT = 100;
    private static final int THREAD_COUNT = 100;
    // THREAD_COUNT × DEDUCT_AMOUNT = INITIAL_BALANCE → 전부 성공하면 잔액 0

    /**
     * 테스트용 지갑 생성 헬퍼.
     * 각 테스트가 서로 다른 userId를 사용하여 격리한다.
     */
    private void createWallet(Long userId, int balance) {
        transactionTemplate.execute(status -> {
            jdbcTemplate.update(
                    "INSERT INTO user_jelly_wallet (user_id, current_balance, version) VALUES (?, ?, 0)",
                    userId, balance);
            return null;
        });
    }

    private int getBalance(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT current_balance FROM user_jelly_wallet WHERE user_id = ?",
                Integer.class, userId);
    }

    // ══════════════════════════════════════════
    //  Test 1: 락 없음 — Lost Update 발생 증명
    // ══════════════════════════════════════════

    @Test
    @DisplayName("락 없음: 동시 차감 시 Lost Update가 발생하여 잔액 정합성이 깨진다")
    void noLock_concurrentDeduct_causesLostUpdate() throws InterruptedException {
        Long userId = 1L;
        createWallet(userId, INITIAL_BALANCE);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    // 락 없이 read → modify → write (Lost Update 취약)
                    Integer balance = jdbcTemplate.queryForObject(
                            "SELECT current_balance FROM user_jelly_wallet WHERE user_id = ?",
                            Integer.class, userId);
                    int newBalance = balance - DEDUCT_AMOUNT;
                    jdbcTemplate.update(
                            "UPDATE user_jelly_wallet SET current_balance = ? WHERE user_id = ?",
                            newBalance, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 무시 — Lost Update는 예외가 아니라 잘못된 결과로 드러난다
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        int finalBalance = getBalance(userId);
        int expectedBalance = INITIAL_BALANCE - (successCount.get() * DEDUCT_AMOUNT);

        // 핵심 단언: Lost Update로 인해 실제 잔액이 기대보다 크다
        // 100번 차감했으면 0이어야 하지만, 동시 읽기 → 동시 쓰기로 일부가 덮어써진다
        assertThat(finalBalance)
                .as("락 없음: Lost Update로 잔액이 기대(%d)보다 크다", expectedBalance)
                .isGreaterThan(expectedBalance);

        // ── 결과 출력 (README 수치 수집용) ──
        int lostUpdates = THREAD_COUNT - ((INITIAL_BALANCE - finalBalance) / DEDUCT_AMOUNT);
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" [락 없음] 테스트 결과");
        System.out.println("──────────────────────────────────────");
        System.out.printf(" 초기 잔액:      %,d%n", INITIAL_BALANCE);
        System.out.printf(" 차감 시도:      %d회 × %d%n", THREAD_COUNT, DEDUCT_AMOUNT);
        System.out.printf(" 성공 횟수:      %d%n", successCount.get());
        System.out.printf(" 최종 잔액:      %,d  ← 0이어야 정상%n", finalBalance);
        System.out.printf(" 기대 잔액:      %,d%n", expectedBalance);
        System.out.printf(" Lost Update:   %d건 손실%n", lostUpdates);
        System.out.println(" 정합성:        ❌ 깨짐");
        System.out.println("══════════════════════════════════════");
        System.out.println();
    }

    // ══════════════════════════════════════════
    //  Test 2: 비관적 락 — 정합성 유지 증명
    // ══════════════════════════════════════════

    @Test
    @DisplayName("비관적 락(FOR UPDATE): 동시 차감 시 모든 요청이 순차 처리되어 잔액이 정확하다")
    void pessimisticLock_concurrentDeduct_maintainsConsistency() throws InterruptedException {
        Long userId = 2L;
        createWallet(userId, INITIAL_BALANCE);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    jellyService.use(userId, DEDUCT_AMOUNT, ReferenceType.MEMBERSHIP, 1L);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(120, TimeUnit.SECONDS)).isTrue();

        int finalBalance = getBalance(userId);

        // 비관적 락: 100개 요청이 순차 처리 → 전부 성공 → 잔액 0
        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(finalBalance).isEqualTo(0);

        // ── 결과 출력 (README 수치 수집용) ──
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" [비관적 락] 테스트 결과");
        System.out.println("──────────────────────────────────────");
        System.out.printf(" 초기 잔액:      %,d%n", INITIAL_BALANCE);
        System.out.printf(" 차감 시도:      %d회 × %d%n", THREAD_COUNT, DEDUCT_AMOUNT);
        System.out.printf(" 성공 횟수:      %d%n", successCount.get());
        System.out.printf(" 실패 횟수:      %d%n", failCount.get());
        System.out.printf(" 최종 잔액:      %,d%n", finalBalance);
        System.out.println(" 정합성:        ✅ 유지");
        System.out.println("══════════════════════════════════════");
        System.out.println();
    }

    // ══════════════════════════════════════════
    //  Test 3: 낙관적 락 — 충돌 감지 + 정합성 증명
    // ══════════════════════════════════════════

    @Test
    @DisplayName("낙관적 락(@Version): 동시 차감 시 일부 실패하지만 성공 건의 정합성은 유지된다")
    void optimisticLock_concurrentDeduct_detectsConflict() throws InterruptedException {
        Long userId = 3L;
        createWallet(userId, INITIAL_BALANCE);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    // findById (FOR UPDATE 없음) → @Version이 충돌 감지
                    transactionTemplate.execute(status -> {
                        UserJellyBalance wallet = repository.findById(userId).orElseThrow();
                        wallet.use(DEDUCT_AMOUNT);
                        repository.saveAndFlush(wallet);
                        return null;
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ObjectOptimisticLockingFailureException 또는 래핑된 예외
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(120, TimeUnit.SECONDS)).isTrue();

        int finalBalance = getBalance(userId);

        // 낙관적 락: 일부는 버전 충돌로 실패
        assertThat(failCount.get())
                .as("낙관적 락: 버전 충돌로 일부 요청이 실패해야 한다")
                .isGreaterThan(0);

        // 핵심: 성공한 건수만큼 정확히 차감 → 데이터 정합성 유지
        int expectedBalance = INITIAL_BALANCE - (successCount.get() * DEDUCT_AMOUNT);
        assertThat(finalBalance)
                .as("낙관적 락: 성공 %d건 × %d = 차감액, 잔액이 정확해야 한다",
                        successCount.get(), DEDUCT_AMOUNT)
                .isEqualTo(expectedBalance);

        // 전체 = 성공 + 실패
        assertThat(successCount.get() + failCount.get()).isEqualTo(THREAD_COUNT);

        // ── 결과 출력 (README 수치 수집용) ──
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" [낙관적 락] 테스트 결과");
        System.out.println("──────────────────────────────────────");
        System.out.printf(" 초기 잔액:      %,d%n", INITIAL_BALANCE);
        System.out.printf(" 차감 시도:      %d회 × %d%n", THREAD_COUNT, DEDUCT_AMOUNT);
        System.out.printf(" 성공 횟수:      %d%n", successCount.get());
        System.out.printf(" 실패 횟수:      %d (ObjectOptimisticLockingFailureException)%n", failCount.get());
        System.out.printf(" 최종 잔액:      %,d%n", finalBalance);
        System.out.printf(" 기대 잔액:      %,d (성공 %d건 × %d)%n",
                expectedBalance, successCount.get(), DEDUCT_AMOUNT);
        System.out.println(" 정합성:        ✅ 유지 (성공한 건수만큼 정확히 차감)");
        System.out.println("══════════════════════════════════════");
        System.out.println();
    }
}
