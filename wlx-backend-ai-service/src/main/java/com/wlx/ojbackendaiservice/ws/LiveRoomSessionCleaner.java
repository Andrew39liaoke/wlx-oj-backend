package com.wlx.ojbackendaiservice.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.Resource;

/**
 * 直播间 WebSocket 会话定时清理器 (对齐交流大厅技术)
 */
@Configuration
@EnableScheduling
public class LiveRoomSessionCleaner {

    @Resource
    private LiveWebSocketHandler liveWebSocketHandler;

    /**
     * 每 30 秒执行一次，清理空闲超时连接
     */
    @Scheduled(fixedRate = 30000)
    public void cleanTimeoutSessions() {
        liveWebSocketHandler.cleanTimeoutSessions();
    }
}
