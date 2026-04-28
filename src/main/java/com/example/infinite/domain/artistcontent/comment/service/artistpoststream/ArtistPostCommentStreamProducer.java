package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.global.common.redis.RedisStreamGroupHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 댓글 비동기 명령을 Redis Stream 에 적재하는 producer 다.
 *
 * producer 는 "큐에 넣는 책임"만 갖고,
 * 실제 DB 반영은 consumer/processor 로 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class ArtistPostCommentStreamProducer {

    public static final String STREAM_KEY = "artist-post:comment:v2:commands";
    public static final String CONSUMER_GROUP = "artist-post-comment-v2-group";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamGroupHelper redisStreamGroupHelper;

    public void enqueue(ArtistPostCommentStreamCommand command) {
        // 댓글 v2도 좋아요 v3와 같은 패턴으로 "HTTP 요청 수신"과 "DB 댓글 쓰기"를 분리한다.
        // group 이 먼저 있어야 consumer 가 처음 붙는 순간부터 pending 메시지를 정상 인계받는다.
        redisStreamGroupHelper.ensureGroup(STREAM_KEY, CONSUMER_GROUP);
        stringRedisTemplate.opsForStream().add(command.toRecord(STREAM_KEY));
    }
}
