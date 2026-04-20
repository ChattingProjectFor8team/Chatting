package com.example.infinite.domain.raffle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RaffleAuditLogger {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY_FORMAT = "raffle:%d:audit-log";
    private static final long MAX_STREAM_LENGTH = 100_000;

    public void log(long raffleId, int slotIndex, long userId, int entryOrder, boolean replaced) {
        String streamKey = String.format(STREAM_KEY_FORMAT, raffleId);

        Map<String, String> fields = Map.of(
                "raffleId", String.valueOf(raffleId),
                "slotIndex", String.valueOf(slotIndex),
                "userId", String.valueOf(userId),
                "entryOrder", String.valueOf(entryOrder),
                "replaced", String.valueOf(replaced),
                "timestamp", Instant.now().toString()
        );

        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in(streamKey)
                .ofMap(fields);

        try {
            stringRedisTemplate.opsForStream().add(record);
            stringRedisTemplate.opsForStream().trim(streamKey, MAX_STREAM_LENGTH, true);
            log.debug("감사 로그 발행: raffleId={}, userId={}, order={}", raffleId, userId, entryOrder);
        } catch (Exception e) {
            log.warn("감사 로그 발행 실패: raffleId={}, userId={}, error={}", raffleId, userId, e.getMessage());
        }
    }
}