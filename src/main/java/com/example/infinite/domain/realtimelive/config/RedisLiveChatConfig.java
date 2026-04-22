package com.example.infinite.domain.realtimelive.config;

import com.example.infinite.domain.realtimelive.service.RedisLiveChatSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@SuppressWarnings("deprecation")
public class RedisLiveChatConfig {

    private static final String REDIS_LIVE_CHAT_CHANNEL = "live:chat:broadcast";

    @Bean
    public ChannelTopic liveChatTopic() {
        return new ChannelTopic(REDIS_LIVE_CHAT_CHANNEL);
    }

    /**
     * 라이브 채팅 Pub/Sub 전용 RedisTemplate.
     * 값 직렬화를 Jackson JSON으로 통일해 구독 측 ObjectMapper.readValue와 호환시킨다.
     */
    @Bean
    public RedisTemplate<String, Object> liveChatRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public MessageListenerAdapter liveChatListenerAdapter(RedisLiveChatSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        // 발행 측이 Jackson으로 JSON 바이트를 보내므로 어댑터도 StringRedisSerializer로 맞춰
        // onMessage(String) 시그니처에 JSON 텍스트가 그대로 전달되도록 한다.
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public RedisMessageListenerContainer liveChatListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter liveChatListenerAdapter,
            ChannelTopic liveChatTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(liveChatListenerAdapter, liveChatTopic);
        return container;
    }
}