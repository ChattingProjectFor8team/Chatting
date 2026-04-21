package com.example.infinite.domain.realtimelive.controller;

import com.example.infinite.domain.realtimelive.dto.LiveChatMessageDto;
import com.example.infinite.domain.realtimelive.service.LiveChatBuffer;
import com.example.infinite.domain.realtimelive.service.LiveChatThrottleService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP /pub/live/{liveId}/chat 메시지 핸들러.
 *
 * 메시지 인입 → 뮤트 체크 → 쓰로틀링 체크 → 200자 체크 → 버퍼 적재
 * 위반 시 조용히 무시 (에러 응답 없음).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LiveChatMessageHandler {

    private final LiveChatThrottleService throttleService;
    private final LiveChatBuffer liveChatBuffer;

    @MessageMapping("/live/{liveId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long liveId,
            @Payload String message,
            Principal principal) {

        Long userId = extractUserId(principal);
        if (userId == null) {
            log.warn("인증 정보 없는 채팅 메시지 무시: liveId={}", liveId);
            return;
        }

        // 3중 검증: 뮤트 → 쓰로틀링 → 글자수
        if (!throttleService.isAllowed(liveId, userId, message)) {
            return; // 조용히 무시
        }

        // 버퍼에 적재 — flush 스케줄러가 300ms마다 배치 전송
        liveChatBuffer.add(LiveChatMessageDto.of(liveId, userId, message));
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof MemberDetailsImpl memberDetails) {
            return memberDetails.getMemberId();
        }
        return null;
    }
}