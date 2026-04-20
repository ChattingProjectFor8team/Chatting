package com.example.infinite.domain.raffle;

import com.example.infinite.domain.raffle.service.ReservoirSampler;
import com.example.infinite.domain.raffle.service.ReservoirSampler.EntryResult;
import com.example.infinite.domain.raffle.service.ReservoirSampler.SlotCloseResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ReservoirSamplerIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ReservoirSampler sampler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ──────────── 단일 스레드 기본 동작 ────────────

    @Test
    @DisplayName("응모 시 순번이 순차 증가하고 후보가 존재한다")
    void singleThreadEntry() {
        long raffleId = 1L;
        int slotIndex = 0;

        for (long userId = 1; userId <= 10; userId++) {
            EntryResult result = sampler.enter(raffleId, slotIndex, userId);
            assertThat(result.success()).isTrue();
            assertThat(result.entryOrder()).isEqualTo((int) userId);
        }

        SlotCloseResult closeResult = sampler.closeSlot(raffleId, slotIndex);
        assertThat(closeResult.totalEntries()).isEqualTo(10);
        assertThat(closeResult.candidateUserId()).isNotNull();
        long candidateId = Long.parseLong(closeResult.candidateUserId());
        assertThat(candidateId).isBetween(1L, 10L);
    }

    @Test
    @DisplayName("중복 응모 시 ALREADY_ENTERED로 거절된다")
    void duplicateEntryBlocked() {
        long raffleId = 2L;
        int slotIndex = 0;
        long userId = 100L;

        EntryResult first = sampler.enter(raffleId, slotIndex, userId);
        EntryResult second = sampler.enter(raffleId, slotIndex, userId);

        assertThat(first.success()).isTrue();
        assertThat(second.success()).isFalse();
        assertThat(second.reason()).isEqualTo("ALREADY_ENTERED");
    }

    // ──────────── 슬롯 마감 원자성 ────────────

    @Test
    @DisplayName("슬롯 마감 후 응모 요청은 SLOT_CLOSED로 거절된다")
    void entryAfterSlotClosed() {
        long raffleId = 3L;
        int slotIndex = 0;

        sampler.enter(raffleId, slotIndex, 1L);
        sampler.enter(raffleId, slotIndex, 2L);

        // 슬롯 마감
        SlotCloseResult closeResult = sampler.closeSlot(raffleId, slotIndex);
        assertThat(closeResult.totalEntries()).isEqualTo(2);

        // 마감 후 응모 시도
        EntryResult late = sampler.enter(raffleId, slotIndex, 3L);
        assertThat(late.success()).isFalse();
        assertThat(late.reason()).isEqualTo("SLOT_CLOSED");
    }

    @Test
    @DisplayName("빈 슬롯 마감 시 후보가 없고 참여자 수가 0이다")
    void emptySlotClose() {
        long raffleId = 4L;
        int slotIndex = 0;

        SlotCloseResult result = sampler.closeSlot(raffleId, slotIndex);
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.candidateUserId()).isNull();
        assertThat(result.totalEntries()).isEqualTo(0);
    }

    // ──────────── 동시성 검증 ────────────

    @Test
    @DisplayName("100개 스레드 동시 응모 시 카운터 정합성이 유지된다")
    void concurrentEntries() throws InterruptedException {
        long raffleId = 5L;
        int slotIndex = 0;
        int threadCount = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executor.submit(() -> {
                try {
                    barrier.await();
                    EntryResult result = sampler.enter(raffleId, slotIndex, userId);
                    if (result.success()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 모든 유저가 서로 다른 userId이므로 전원 응모 성공
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Lua INCR은 원자적이므로 카운터는 정확히 threadCount
        SlotCloseResult closeResult = sampler.closeSlot(raffleId, slotIndex);
        assertThat(closeResult.totalEntries()).isEqualTo(threadCount);
        assertThat(closeResult.candidateUserId()).isNotNull();
    }

    @Test
    @DisplayName("동일 유저가 여러 스레드에서 동시 응모해도 1회만 성공한다")
    void concurrentDuplicateEntry() throws InterruptedException {
        long raffleId = 6L;
        int slotIndex = 0;
        long userId = 999L;
        int threadCount = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    EntryResult result = sampler.enter(raffleId, slotIndex, userId);
                    if (result.success()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // SETNX 원자성에 의해 정확히 1회만 성공
        assertThat(successCount.get()).isEqualTo(1);
    }

    // ──────────── 키 정리 ────────────

    @Test
    @DisplayName("cleanupSlotKeys 호출 후 슬롯 관련 키가 삭제된다")
    void cleanupSlotKeys() {
        long raffleId = 7L;
        int slotIndex = 0;

        sampler.enter(raffleId, slotIndex, 1L);
        sampler.closeSlot(raffleId, slotIndex);

        // 정리 전: 키 존재
        assertThat(stringRedisTemplate.hasKey("{raffle:7:slot:0}:closed")).isTrue();

        // 정리
        sampler.cleanupSlotKeys(raffleId, slotIndex);

        // 정리 후: 키 삭제됨
        assertThat(stringRedisTemplate.hasKey("{raffle:7:slot:0}:closed")).isFalse();
        assertThat(stringRedisTemplate.hasKey("{raffle:7:slot:0}:count")).isFalse();
        assertThat(stringRedisTemplate.hasKey("{raffle:7:slot:0}:candidate")).isFalse();
    }
}
