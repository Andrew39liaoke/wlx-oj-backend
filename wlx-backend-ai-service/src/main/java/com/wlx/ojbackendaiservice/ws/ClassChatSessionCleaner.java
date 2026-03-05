package com.wlx.ojbackendaiservice.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.Resource;

@Configuration
@EnableScheduling
public class ClassChatSessionCleaner {

    @Resource
    private ClassChatWebSocketHandler classChatWebSocketHandler;

    /**
     * 每 30 秒执行一次，清理空闲超时连接
     */
    @Scheduled(fixedRate = 30000)
    public void cleanTimeoutSessions() {
        classChatWebSocketHandler.cleanTimeoutSessions();
    }
}
