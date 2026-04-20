package com.example.infinite.domain.realtimelive.config;

import com.example.infinite.domain.realtimelive.service.RedisLiveChatSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisLiveChatConfig {

    private static final String REDIS_LIVE_CHAT_CHANNEL = "live:chat:broadcast";

    @Bean
    public ChannelTopic liveChatTopic() {
        return new ChannelTopic(REDIS_LIVE_CHAT_CHANNEL);
    }

    @Bean
    public MessageListenerAdapter liveChatListenerAdapter(RedisLiveChatSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
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