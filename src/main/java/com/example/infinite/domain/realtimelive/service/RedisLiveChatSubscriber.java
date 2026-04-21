package com.example.infinite.domain.realtimelive.service;

import com.example.infinite.domain.realtimelive.dto.LiveChatMessageDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis Pub/Sub으로 다른 서버 인스턴스에서 전파된 채팅 메시지를 수신하여
 * 현재 인스턴스의 STOMP 구독자에게 전달한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLiveChatSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void onMessage(String message) {
        try {
            List<LiveChatMessageDto> messages = objectMapper.readValue(
                    message, new TypeReference<>() {});

            if (!messages.isEmpty()) {
                Long liveId = messages.get(0).liveId();
                messagingTemplate.convertAndSend("/sub/live/" + liveId, messages);
            }
        } catch (Exception e) {
            log.error("Redis Pub/Sub 메시지 처리 실패: {}", e.getMessage());
        }
    }
}