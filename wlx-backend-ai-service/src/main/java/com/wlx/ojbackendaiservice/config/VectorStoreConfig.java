package com.wlx.ojbackendaiservice.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Redis VectorStore 配置类
 * 手动配置 VectorStore Bean 以解决自动配置失败的问题
 */
@Configuration
public class VectorStoreConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.ai.vectorStore.redis.prefix}")
    private String prefix;

    @Value("${spring.ai.vectorStore.redis.index-name}")
    private String indexName;
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(host, port);
    }

    @Bean
    public RedisVectorStore vectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .prefix(prefix)
                .indexName(indexName)
                .initializeSchema(true)  // 添加这一行，自动创建索引
                .build();
    }
}
