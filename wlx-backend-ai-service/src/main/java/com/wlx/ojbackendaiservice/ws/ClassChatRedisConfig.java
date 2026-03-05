package com.wlx.ojbackendaiservice.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import jakarta.annotation.Resource;

@Configuration
public class ClassChatRedisConfig {

    @Resource
    private ClassChatRedisListener classChatRedisListener;

    @Bean
    public RedisMessageListenerContainer classChatRedisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 订阅所有班级聊天的 Topic
        container.addMessageListener(classChatRedisListener, new PatternTopic("class_chat_channel:*"));
        return container;
    }
}
