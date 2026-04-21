package com.example.infinite.domain.realtimelive.service;

import com.example.infinite.domain.realtimelive.dto.LiveChatMessageDto;
import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;
import com.example.infinite.domain.realtimelive.repository.LiveChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LiveChatBroadcastService {

    private static final String REDIS_LIVE_CHAT_CHANNEL = "live:chat:broadcast";

    private final LiveChatBuffer liveChatBuffer;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> liveChatRedisTemplate;
    private final LiveChatMessageRepository liveChatMessageRepository;

    public LiveChatBroadcastService(
            LiveChatBuffer liveChatBuffer,
            SimpMessagingTemplate messagingTemplate,
            @Qualifier("liveChatRedisTemplate") RedisTemplate<String, Object> liveChatRedisTemplate,
            LiveChatMessageRepository liveChatMessageRepository) {
        this.liveChatBuffer = liveChatBuffer;
        this.messagingTemplate = messagingTemplate;
        this.liveChatRedisTemplate = liveChatRedisTemplate;
        this.liveChatMessageRepository = liveChatMessageRepository;
    }

    /**
     * 300ms 간격으로 버퍼를 flush하여 구독자에게 일괄 전송 + DB 저장 + Redis Pub/Sub 전파.
     */
    @Scheduled(fixedRate = 300)
    public void flush() {
        Map<Long, List<LiveChatMessageDto>> flushed = liveChatBuffer.flushAll();

        flushed.forEach((liveId, messages) -> {
            // 1. STOMP 구독자에게 배치 전송
            messagingTemplate.convertAndSend("/sub/live/" + liveId, messages);

            // 2. Redis Pub/Sub으로 다른 서버 인스턴스에 전파
            try {
                liveChatRedisTemplate.convertAndSend(REDIS_LIVE_CHAT_CHANNEL, messages);
            } catch (Exception e) {
                log.warn("Redis Pub/Sub 전파 실패: liveId={}, error={}", liveId, e.getMessage());
            }

            // 3. DB Batch INSERT
            try {
                List<LiveChatMessage> entities = messages.stream()
                        .map(msg -> LiveChatMessage.builder()
                                .liveStreamId(msg.liveId())
                                .senderUserId(msg.senderId())
                                .message(msg.message())
                                .build())
                        .toList();
                liveChatMessageRepository.saveAll(entities);
            } catch (Exception e) {
                log.error("채팅 DB 저장 실패: liveId={}, count={}, error={}",
                        liveId, messages.size(), e.getMessage());
            }
        });
    }
}