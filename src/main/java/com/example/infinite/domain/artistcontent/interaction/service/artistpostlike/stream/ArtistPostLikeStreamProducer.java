package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import com.example.infinite.global.common.redis.RedisStreamGroupHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistPostLikeStreamProducer {

    public static final String STREAM_KEY = "artist-post:like:v3:commands";
    public static final String CONSUMER_GROUP = "artist-post-like-v3-group";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamGroupHelper redisStreamGroupHelper;

    public void enqueue(ArtistPostLikeStreamCommand command) {
        // group을 보장한 뒤 stream에 적재해야 첫 burst에서도 메시지 유실 없이 consumer가 이어받는다.
        // 여기서는 DB connection을 쓰지 않고 Redis append만 수행하므로 burst 흡수력이 훨씬 좋다.
        redisStreamGroupHelper.ensureGroup(STREAM_KEY, CONSUMER_GROUP);
        stringRedisTemplate.opsForStream().add(command.toRecord(STREAM_KEY));
    }
}
