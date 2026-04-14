package com.example.infinite.domain.Raffle.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservoirSampler {

    private final StringRedisTemplate stringRedisTemplate;

    // TTL: 현재 하드코딩 24시간. Phase 2에서 래플 duration 기반 동적 TTL로 교체 예정.
    private static final Duration DEFAULT_KEY_TTL = Duration.ofDays(1);

    // 중복 응모 방지 키
    private static final String USER_ENTRY_KEY_FORMAT = "raffle:%d:user:%d";

    // 슬롯별 키 (Hash Tag 적용 — Redis Cluster 대비)
    private static final String CLOSED_KEY_FORMAT = "{raffle:%d:slot:%d}:closed";
    private static final String COUNT_KEY_FORMAT = "{raffle:%d:slot:%d}:count";
    private static final String CANDIDATE_KEY_FORMAT = "{raffle:%d:slot:%d}:candidate";

    // Lua Script
    private DefaultRedisScript<List> entryScript;
    private DefaultRedisScript<List> closeSlotScript;

    @PostConstruct
    void initScripts() {
        entryScript = new DefaultRedisScript<>();
        entryScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/entry.lua")));
        entryScript.setResultType(List.class);

        closeSlotScript = new DefaultRedisScript<>();
        closeSlotScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/close_slot.lua")));
        closeSlotScript.setResultType(List.class);
    }

    /**
     * 응모 결과를 담는 record.
     * @param success     응모 성공 여부
     * @param entryOrder  순번 (-1이면 슬롯 마감)
     * @param replaced    candidate가 교체되었는지 여부
     * @param reason      실패 사유 (성공 시 null)
     */
    public record EntryResult(boolean success, int entryOrder, boolean replaced, String reason) {
        public static EntryResult rejected(String reason) {
            return new EntryResult(false, -1, false, reason);
        }
        public static EntryResult entered(int order, boolean replaced) {
            return new EntryResult(true, order, replaced, null);
        }
    }

    /**
     * 응모 처리.
     * 1. 중복 응모 체크 (SETNX + TTL)
     * 2. entry.lua 실행 (closed 확인 + INCR + 확률 판정 + 조건부 candidate SET)
     */
    public EntryResult enter(long raffleId, int slotIndex, long userId) {
        // 1. 중복 응모 방지
        String userEntryKey = String.format(USER_ENTRY_KEY_FORMAT, raffleId, userId);
        Boolean isFirstEntry = stringRedisTemplate.opsForValue()
                .setIfAbsent(userEntryKey, String.valueOf(slotIndex), DEFAULT_KEY_TTL);

        if (Boolean.FALSE.equals(isFirstEntry)) {
            log.debug("중복 응모 차단: raffleId={}, userId={}", raffleId, userId);
            return EntryResult.rejected("ALREADY_ENTERED");
        }

        // 2. Lua Script 실행 (원자적)
        String closedKey = String.format(CLOSED_KEY_FORMAT, raffleId, slotIndex);
        String countKey = String.format(COUNT_KEY_FORMAT, raffleId, slotIndex);
        String candidateKey = String.format(CANDIDATE_KEY_FORMAT, raffleId, slotIndex);

        double rand = ThreadLocalRandom.current().nextDouble(); // 0.0 ~ 1.0

        List<Long> result = stringRedisTemplate.execute(
                entryScript,
                Arrays.asList(closedKey, countKey, candidateKey),
                String.valueOf(userId),
                String.valueOf(rand)
        );

        if (result == null || result.isEmpty()) {
            log.error("entry.lua 실행 실패: raffleId={}, slotIndex={}", raffleId, slotIndex);
            // 중복 응모 키 롤백
            stringRedisTemplate.delete(userEntryKey);
            return EntryResult.rejected("SCRIPT_ERROR");
        }

        int entryOrder = result.get(0).intValue();
        boolean replaced = result.get(1).intValue() == 1;

        // 슬롯 마감으로 거절된 경우
        if (entryOrder == -1) {
            // 중복 응모 키 롤백 (이 슬롯에서 거절되었으므로 다음 슬롯에서 재시도 가능해야 함)
            stringRedisTemplate.delete(userEntryKey);
            log.debug("슬롯 마감으로 응모 거절: raffleId={}, slotIndex={}, userId={}",
                    raffleId, slotIndex, userId);
            return EntryResult.rejected("SLOT_CLOSED");
        }

        // count 키에 첫 생성 시 TTL 설정
        if (entryOrder == 1) {
            stringRedisTemplate.expire(countKey, DEFAULT_KEY_TTL);
            stringRedisTemplate.expire(candidateKey, DEFAULT_KEY_TTL);
        }

        log.debug("응모 성공: raffleId={}, slotIndex={}, userId={}, order={}, replaced={}",
                raffleId, slotIndex, userId, entryOrder, replaced);
        return EntryResult.entered(entryOrder, replaced);
    }

    /**
     * 슬롯 종료 처리 (스케줄러 호출).
     * close_slot.lua를 실행하여 원자적으로 슬롯을 닫고 최종 후보/참여자 수를 읽는다.
     */
    public SlotCloseResult closeSlot(long raffleId, int slotIndex) {
        String closedKey = String.format(CLOSED_KEY_FORMAT, raffleId, slotIndex);
        String candidateKey = String.format(CANDIDATE_KEY_FORMAT, raffleId, slotIndex);
        String countKey = String.format(COUNT_KEY_FORMAT, raffleId, slotIndex);

        List<Object> result = stringRedisTemplate.execute(
                closeSlotScript,
                Arrays.asList(closedKey, candidateKey, countKey)
        );

        if (result == null || result.isEmpty()) {
            log.error("close_slot.lua 실행 실패: raffleId={}, slotIndex={}", raffleId, slotIndex);
            return new SlotCloseResult(null, 0);
        }

        String candidate = result.get(0) instanceof String s && !s.isEmpty() ? s : null;
        int count = result.get(1) instanceof Long l ? l.intValue() : 0;

        log.info("슬롯 종료: raffleId={}, slotIndex={}, candidate={}, count={}",
                raffleId, slotIndex, candidate, count);
        return new SlotCloseResult(candidate, count);
    }

    /**
     * 슬롯 종료 결과.
     * @param candidateUserId 최종 당첨 후보 userId (참여자 없으면 null)
     * @param totalEntries    총 참여자 수
     */
    public record SlotCloseResult(String candidateUserId, int totalEntries) {
        public boolean isEmpty() {
            return candidateUserId == null || totalEntries == 0;
        }
    }

    /**
     * 슬롯 관련 Redis 키 정리 (슬롯 종료 후 호출).
     * 스케줄러가 DB 쓰기 완료 후 호출한다.
     * TTL이 보험으로 걸려 있으므로, 이 메서드가 호출되지 않아도 키는 만료된다.
     */
    public void cleanupSlotKeys(long raffleId, int slotIndex) {
        String closedKey = String.format(CLOSED_KEY_FORMAT, raffleId, slotIndex);
        String countKey = String.format(COUNT_KEY_FORMAT, raffleId, slotIndex);
        String candidateKey = String.format(CANDIDATE_KEY_FORMAT, raffleId, slotIndex);

        stringRedisTemplate.unlink(Arrays.asList(closedKey, countKey, candidateKey));
        log.debug("슬롯 키 정리 완료: raffleId={}, slotIndex={}", raffleId, slotIndex);
    }

    /**
     * Reservoir Sampling 확률 판정 (k=1) — 단위 테스트 전용 순수 함수.
     *
     * 실제 응모 경로에서는 이 메서드를 사용하지 않는다.
     * entry.lua 내부에서 동일한 로직이 원자적으로 실행된다.
     *
     * 이 메서드가 존재하는 이유:
     * - Lua Script 내부의 확률 로직을 Java로 미러링하여 Redis 없이 10만 회 시뮬레이션으로
     *   1/n 균등 분포 수렴을 통계적으로 검증하기 위함.
     * - Lua 코드의 수학적 정확성에 대한 신뢰 근거를 제공한다.
     */
    boolean shouldReplace(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count는 1 이상이어야 합니다: " + count);
        }
        if (count == 1) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(count) == 0;
    }
}
