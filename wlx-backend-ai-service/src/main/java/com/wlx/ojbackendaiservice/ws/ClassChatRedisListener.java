package com.wlx.ojbackendaiservice.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
@Slf4j
public class ClassChatRedisListener implements MessageListener {

    @Resource
    private ClassChatWebSocketHandler classChatWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            // channel 格式为: class_chat_channel:{classId}
            String[] parts = channel.split(":");
            if (parts.length == 2) {
                Long classId = Long.parseLong(parts[1]);
                // 下发给连接到本节点的相应班级的 WebSocketSession
                classChatWebSocketHandler.broadcastToLocal(classId, body);
            }
        } catch (Exception e) {
            log.error("处理Redis订阅群聊消息异常", e);
        }
    }
}
