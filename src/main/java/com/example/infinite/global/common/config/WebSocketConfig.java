package com.example.infinite.global.common.config;

import com.example.infinite.global.auth.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 경로: /sub/live/{liveId}, /sub/dm/{roomId}, /sub/user/{userId}/notifications
        registry.enableSimpleBroker("/sub");
        // 발행 경로: /pub/live/{liveId}/chat, /pub/dm/{roomId}
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SecurityConfig에서 /ws-stomp/** permitAll() 처리됨
        // JWT 인증은 ChannelInterceptor에서 CONNECT 시점에 수행
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 시 JWT 검증 인터셉터 등록
        registration.interceptors(stompAuthChannelInterceptor);
    }
}