package com.example.infinite.domain.raffle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RaffleAuditConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

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

        // DB Batch INSERT
        batchInsert(records);

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

    private void batchInsert(List<MapRecord<String, Object, Object>> records) {
        String sql = "INSERT INTO raffle_audit_logs (raffle_id, user_id, slot_index, entry_order, replaced, event_timestamp) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.batchUpdate(sql, records, BATCH_SIZE, (ps, record) -> {
                Map<Object, Object> fields = record.getValue();
                ps.setLong(1, Long.parseLong((String) fields.get("raffleId")));
                ps.setLong(2, Long.parseLong((String) fields.get("userId")));
                ps.setInt(3, Integer.parseInt((String) fields.get("slotIndex")));
                ps.setInt(4, Integer.parseInt((String) fields.get("entryOrder")));
                ps.setBoolean(5, Boolean.parseBoolean((String) fields.get("replaced")));
                ps.setTimestamp(6, Timestamp.from(Instant.parse((String) fields.get("timestamp"))));
            });
            log.info("감사 로그 {} 건 DB 저장 완료", records.size());
        } catch (Exception e) {
            log.error("감사 로그 DB 저장 실패: {} 건 손실 가능, error={}", records.size(), e.getMessage());
            // ACK는 이미 진행되므로, 실패 시 로그로 남긴다.
            // 운영 환경에서는 DLQ(Dead Letter Queue) 또는 재시도 로직 필요.
        }
    }
}