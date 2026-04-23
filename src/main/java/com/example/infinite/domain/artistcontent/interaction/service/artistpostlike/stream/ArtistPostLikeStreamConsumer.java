package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

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
public class ArtistPostLikeStreamConsumer {

    private static final String CONSUMER_NAME = "artist-post-like-v3-consumer";
    private static final int BATCH_SIZE = 200;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration RETRY_STATE_TTL = Duration.ofHours(1);
    private static final String DLQ_STREAM_KEY = "artist-post:like:v3:dlq";

    private final StringRedisTemplate stringRedisTemplate;
    private final ArtistPostLikeStreamProcessor processor;

    /**
     * poll 기반 consumer.
     *
     * 장점:
     * - 구조가 단순하고 Spring Scheduler만으로도 구현 가능
     * - producer와 DB write를 분리해 burst를 Redis Stream이 먼저 흡수한다
     *
     * 현재는 단일 consumer 이름을 사용해 순서를 단순화했다.
     * scale-out 시에는 consumer group 분산, pending 재처리 전략까지 추가 설계가 필요하다.
     */
    @Scheduled(fixedDelayString = "${artist-post.like-v3.consumer-delay-ms:300}")
    public void consume() {
        // 먼저 같은 consumer에 남아 있는 PEL(pending entries)을 재시도한다.
        // 그렇지 않으면 한 번 실패한 메시지는 lastConsumed() 경로에서 다시 안 읽혀 영구 미처리 상태가 된다.
        //
        // 중요:
        // 예전처럼 "pending 이 하나라도 있으면 여기서 종료"해 버리면
        // 독성 메시지(poison message) 1개 때문에 뒤의 정상 새 메시지가 계속 굶을 수 있다.
        // 그래서 pending 을 먼저 처리하되, 그 결과와 무관하게 new message도 계속 읽는다.
        consumeRecords(readPendingRecords());
        consumeRecords(readNewRecords());
    }

    private List<MapRecord<String, Object, Object>> readPendingRecords() {
        try {
            return stringRedisTemplate.opsForStream().read(
                    Consumer.from(ArtistPostLikeStreamProducer.CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_SIZE),
                    StreamOffset.create(ArtistPostLikeStreamProducer.STREAM_KEY, ReadOffset.from("0"))
            );
        } catch (Exception e) {
            log.warn("ArtistPost like pending stream read failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<MapRecord<String, Object, Object>> readNewRecords() {
        try {
            return stringRedisTemplate.opsForStream().read(
                    Consumer.from(ArtistPostLikeStreamProducer.CONSUMER_GROUP, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofMillis(100)),
                    StreamOffset.create(ArtistPostLikeStreamProducer.STREAM_KEY, ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.warn("ArtistPost like stream read failed: {}", e.getMessage());
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
                processor.process(ArtistPostLikeStreamCommand.from(record));
                clearRetryState(record);
                acknowledgeAndDelete(record);
            } catch (Exception e) {
                long retryCount = increaseRetryCount(record);
                log.error(
                        "ArtistPost like stream process failed: recordId={}, retryCount={}, error={}",
                        record.getId(),
                        retryCount,
                        e.getMessage(),
                        e
                );

                // 재시도 횟수가 임계치를 넘으면 DLQ로 격리하고 본 스트림에서는 제거한다.
                // 이렇게 해야 pending 하나가 전체 스트림을 무한히 막는 상황을 피할 수 있다.
                if (retryCount > MAX_RETRY_COUNT) {
                    moveToDeadLetter(record, e, retryCount);
                    clearRetryState(record);
                    acknowledgeAndDelete(record);
                }
            }
        }
    }

    /**
     * 현재 구조는 consumer group 을 하나로만 쓰므로
     * 성공 처리 후에는 ACK 뿐 아니라 원본 엔트리 삭제까지 같이 수행한다.
     *
     * ACK 만 하면 PEL 에서는 빠지지만 Stream 메모리에는 계속 남기 때문에
     * 고트래픽 환경에서는 stream 길이가 무한히 커질 수 있다.
     */
    private void acknowledgeAndDelete(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(
                ArtistPostLikeStreamProducer.STREAM_KEY,
                ArtistPostLikeStreamProducer.CONSUMER_GROUP,
                record.getId()
        );
        stringRedisTemplate.opsForStream().delete(
                ArtistPostLikeStreamProducer.STREAM_KEY,
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

    /**
     * DLQ는 "나중에 사람이 보고 복구할 수 있게 남겨두는 격리 구역"이다.
     * 원본 payload를 최대한 보존하고, 왜 떨어졌는지 최소한의 실패 메타데이터를 덧붙인다.
     */
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
        return "artist-post:like:v3:retry:" + record.getId().getValue();
    }
}
