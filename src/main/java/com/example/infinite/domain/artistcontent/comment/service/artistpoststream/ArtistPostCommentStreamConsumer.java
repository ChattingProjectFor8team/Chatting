package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPostCommentStreamConsumer {

    private static final String CONSUMER_NAME = "artist-post-comment-v2-consumer";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration RETRY_STATE_TTL = Duration.ofHours(1);
    private static final String DLQ_STREAM_KEY = "artist-post:comment:v2:dlq";

    private final StringRedisTemplate stringRedisTemplate;
    private final ArtistPostCommentStreamProcessor processor;

    /**
     * 댓글 v2 consumer.
     *
     * 댓글은 좋아요보다 복잡해서 stream 소비 이후에도 root thread lock이 필요하다.
     * 그래서 consumer는 command type에 따라
     * - root create
     * - reply create with thread lock
     * - delete with thread lock
     * 으로 분기한다.
     */
    @Scheduled(fixedDelayString = "${artist-post.comment-v2.consumer-delay-ms:300}")
    public void consume() {
        // pending 을 먼저 재처리하되, 독성 메시지 하나 때문에 새 댓글 명령이 굶지 않게
        // 이번 poll 에서 new message도 계속 읽는다.
        consumeRecords(readPendingRecords());
        consumeRecords(readNewRecords());
    }

    private List<MapRecord<String, Object, Object>> readPendingRecords() {
        try {
            return stringRedisTemplate.opsForStream().read(
                    Consumer.from(ArtistPostCommentStreamProducer.CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_SIZE),
                    StreamOffset.create(ArtistPostCommentStreamProducer.STREAM_KEY, ReadOffset.from("0"))
            );
        } catch (Exception e) {
            log.warn("ArtistPost comment pending stream read failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<MapRecord<String, Object, Object>> readNewRecords() {
        try {
            return stringRedisTemplate.opsForStream().read(
                    Consumer.from(ArtistPostCommentStreamProducer.CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofMillis(100)),
                    StreamOffset.create(ArtistPostCommentStreamProducer.STREAM_KEY, ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.warn("ArtistPost comment stream read failed: {}", e.getMessage());
            return List.of();
        }
    }

    private void consumeRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> fields = record.getValue();
            if (fields.containsKey("__bootstrap__")) {
                acknowledgeAndDelete(record);
                continue;
            }

            try {
                processor.process(ArtistPostCommentStreamCommand.from(record));
                clearRetryState(record);
                acknowledgeAndDelete(record);
            } catch (Exception e) {
                long retryCount = increaseRetryCount(record);
                log.error(
                        "ArtistPost comment stream process failed: recordId={}, retryCount={}, error={}",
                        record.getId(),
                        retryCount,
                        e.getMessage(),
                        e
                );

                if (retryCount > MAX_RETRY_COUNT) {
                    moveToDeadLetter(record, e, retryCount);
                    clearRetryState(record);
                    acknowledgeAndDelete(record);
                }
            }
        }
    }

    /**
     * 현재 댓글 stream 도 단일 consumer group 전제이므로
     * 성공 처리 후 ACK 만 하지 말고 원본 엔트리도 삭제해 메모리 누적을 막는다.
     */
    private void acknowledgeAndDelete(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(
                ArtistPostCommentStreamProducer.STREAM_KEY,
                ArtistPostCommentStreamProducer.CONSUMER_GROUP,
                record.getId()
        );
        stringRedisTemplate.opsForStream().delete(
                ArtistPostCommentStreamProducer.STREAM_KEY,
                record.getId()
        );
    }

    private long increaseRetryCount(MapRecord<String, Object, Object> record) {
        String retryKey = buildRetryKey(record);
        Long retryCount = stringRedisTemplate.opsForValue().increment(retryKey);
        stringRedisTemplate.expire(retryKey, RETRY_STATE_TTL);
        return retryCount == null ? 1L : retryCount;
    }

    private void clearRetryState(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.delete(buildRetryKey(record));
    }

    private void moveToDeadLetter(MapRecord<String, Object, Object> record, Exception exception, long retryCount) {
        Map<String, String> deadLetterFields = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : record.getValue().entrySet()) {
            deadLetterFields.put(entry.getKey().toString(), entry.getValue().toString());
        }
        deadLetterFields.put("originalRecordId", record.getId().getValue());
        deadLetterFields.put("retryCount", Long.toString(retryCount));
        deadLetterFields.put("failedAt", Instant.now().toString());
        deadLetterFields.put("error", exception.getClass().getSimpleName());
        deadLetterFields.put("errorMessage", exception.getMessage() == null ? "" : exception.getMessage());

        stringRedisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(DLQ_STREAM_KEY)
                        .ofMap(deadLetterFields)
        );
    }

    private String buildRetryKey(MapRecord<String, Object, Object> record) {
        return "artist-post:comment:v2:retry:" + record.getId().getValue();
    }
}
