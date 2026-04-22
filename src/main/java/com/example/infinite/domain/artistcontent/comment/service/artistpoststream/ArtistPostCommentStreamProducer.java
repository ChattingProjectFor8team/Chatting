package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.global.common.redis.RedisStreamGroupHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistPostCommentStreamProducer {

    public static final String STREAM_KEY = "artist-post:comment:v2:commands";
    public static final String CONSUMER_GROUP = "artist-post-comment-v2-group";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamGroupHelper redisStreamGroupHelper;

    public void enqueue(ArtistPostCommentStreamCommand command) {
        // 댓글 v2도 좋아요 v3와 같은 패턴으로 "HTTP 요청 수신"과 "DB 댓글 쓰기"를 분리한다.
        redisStreamGroupHelper.ensureGroup(STREAM_KEY, CONSUMER_GROUP);
        stringRedisTemplate.opsForStream().add(command.toRecord(STREAM_KEY));
    }
}
