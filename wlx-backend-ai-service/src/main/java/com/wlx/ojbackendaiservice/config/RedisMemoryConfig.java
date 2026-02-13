package com.wlx.ojbackendaiservice.config;

import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class RedisMemoryConfig
{
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public JedisRedisChatMemoryRepository redisChatMemoryRepository()
    {
        return JedisRedisChatMemoryRepository.builder()
                    .host(host)
                    .port(port)
                .build();
    }
}
