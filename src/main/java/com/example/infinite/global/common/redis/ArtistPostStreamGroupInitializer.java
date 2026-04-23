package com.example.infinite.global.common.redis;

import com.example.infinite.domain.artistcontent.comment.service.artistpoststream.ArtistPostCommentStreamProducer;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistPostStreamGroupInitializer {

    private final RedisStreamGroupHelper redisStreamGroupHelper;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGroups() {
        // producer가 처음 호출되기 전에도 consumer가 안전하게 읽을 수 있도록
        // ArtistPost stream group을 앱 기동 시점에 미리 보장한다.
        redisStreamGroupHelper.ensureGroup(
                ArtistPostLikeStreamProducer.STREAM_KEY,
                ArtistPostLikeStreamProducer.CONSUMER_GROUP
        );
        redisStreamGroupHelper.ensureGroup(
                ArtistPostCommentStreamProducer.STREAM_KEY,
                ArtistPostCommentStreamProducer.CONSUMER_GROUP
        );
    }
}
