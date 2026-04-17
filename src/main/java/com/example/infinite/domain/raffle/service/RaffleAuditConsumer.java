package com.example.infinite.domain.raffle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RaffleAuditConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CONSUMER_GROUP = "raffle-audit-group";
    private static final String CONSUMER_NAME = "consumer-1"; // Scale-out 시 서버 ID로 변경
    private static final int BATCH_SIZE = 500;

    public void createConsumerGroupIfNotExists(String streamKey) {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), CONSUMER_GROUP);
            log.info("Consumer Group 생성: streamKey={}, group={}", streamKey, CONSUMER_GROUP);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer Group 이미 존재: streamKey={}", streamKey);
            } else {
                log.warn("Consumer Group 생성 실패: streamKey={}, error={}", streamKey, e.getMessage());
            }
        }
    }

    public int consume(String streamKey) {
        List<MapRecord<String, Object, Object>> records;

        try {
            records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofMillis(500)),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.warn("스트림 읽기 실패: streamKey={}, error={}", streamKey, e.getMessage());
            return 0;
        }

        if (records == null || records.isEmpty()) {
            return 0;
        }

        // ──────────────────────────────────────────
        // TODO [Phase 1]: 여기에 DB Batch INSERT 구현
        // JPA 엔티티 확정 후 JdbcTemplate.batchUpdate()로 교체
        // ──────────────────────────────────────────

        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> fields = record.getValue();
            log.info("감사 로그 소비: raffleId={}, userId={}, slotIndex={}, order={}, replaced={}",
                    fields.get("raffleId"),
                    fields.get("userId"),
                    fields.get("slotIndex"),
                    fields.get("entryOrder"),
                    fields.get("replaced"));
        }

        records.forEach(record ->
                stringRedisTemplate.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, record.getId()));

        log.debug("감사 로그 {} 건 소비 완료: streamKey={}", records.size(), streamKey);
        return records.size();
    }

    public void deleteStream(long raffleId) {
        String streamKey = String.format("raffle:%d:audit-log", raffleId);
        stringRedisTemplate.delete(streamKey);
        log.info("감사 로그 스트림 삭제: raffleId={}", raffleId);
    }
}