package com.example.infinite.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // CONNECT 시점에만 JWT 검증 수행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);

            if (!StringUtils.hasText(token)) {
                log.warn("STOMP CONNECT: Authorization 헤더 없음");
                throw new IllegalArgumentException("STOMP 연결에 JWT 토큰이 필요합니다.");
            }

            try {
                Claims claims = jwtTokenProvider.validateToken(token);
                Authentication authentication = jwtTokenProvider.getAuthentication(claims);
                // 인증 정보를 STOMP 세션에 바인딩
                // 이후 메시지에서 accessor.getUser()로 접근 가능
                accessor.setUser(authentication);
            } catch (ExpiredJwtException e) {
                log.warn("STOMP CONNECT: 만료된 토큰 - {}", e.getMessage());
                throw new IllegalArgumentException("만료된 토큰입니다.");
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("STOMP CONNECT: 유효하지 않은 토큰 - {}", e.getMessage());
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        // STOMP 헤더에서 Authorization: Bearer xxx 추출
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}