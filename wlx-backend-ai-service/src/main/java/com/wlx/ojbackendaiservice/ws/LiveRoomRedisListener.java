package com.wlx.ojbackendaiservice.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 直播间 Redis 消息监听器
 */
@Component
@Slf4j
public class LiveRoomRedisListener implements MessageListener {

    @Resource
    private LiveWebSocketHandler liveWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            // channel 格式为: live_room_channel:{roomId}
            String[] parts = channel.split(":");
            if (parts.length == 2) {
                String roomId = parts[1];
                liveWebSocketHandler.broadcastToLocal(roomId, body);
            }
        } catch (Exception e) {
            log.error("处理Redis直播消息异常", e);
        }
    }
}
