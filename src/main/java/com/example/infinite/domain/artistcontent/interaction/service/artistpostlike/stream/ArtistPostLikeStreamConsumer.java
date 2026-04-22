package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPostLikeStreamConsumer {

    private static final String CONSUMER_NAME = "artist-post-like-v3-consumer";
    private static final int BATCH_SIZE = 200;

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
        if (consumeRecords(readPendingRecords())) {
            return;
        }

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

    private boolean consumeRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) {
            return false;
        }

        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> fields = record.getValue();
            if (fields.containsKey("__bootstrap__")) {
                acknowledge(record);
                continue;
            }

            try {
                processor.process(ArtistPostLikeStreamCommand.from(record));
                acknowledge(record);
            } catch (Exception e) {
                log.error("ArtistPost like stream process failed: recordId={}, error={}", record.getId(), e.getMessage(), e);
            }
        }

        return true;
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(
                ArtistPostLikeStreamProducer.STREAM_KEY,
                ArtistPostLikeStreamProducer.CONSUMER_GROUP,
                record.getId()
        );
    }
}
