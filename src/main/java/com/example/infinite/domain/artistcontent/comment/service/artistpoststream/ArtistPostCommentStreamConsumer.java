package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

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
public class ArtistPostCommentStreamConsumer {

    private static final String CONSUMER_NAME = "artist-post-comment-v2-consumer";
    private static final int BATCH_SIZE = 100;

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
        if (consumeRecords(readPendingRecords())) {
            return;
        }

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
                processor.process(ArtistPostCommentStreamCommand.from(record));
                acknowledge(record);
            } catch (Exception e) {
                log.error("ArtistPost comment stream process failed: recordId={}, error={}", record.getId(), e.getMessage(), e);
            }
        }

        return true;
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        stringRedisTemplate.opsForStream().acknowledge(
                ArtistPostCommentStreamProducer.STREAM_KEY,
                ArtistPostCommentStreamProducer.CONSUMER_GROUP,
                record.getId()
        );
    }
}
