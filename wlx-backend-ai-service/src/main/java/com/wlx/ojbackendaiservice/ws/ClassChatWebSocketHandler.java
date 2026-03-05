package com.wlx.ojbackendaiservice.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendaiservice.service.ClassChatMessageService;
import com.wlx.ojbackendmodel.model.entity.ClassChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class ClassChatWebSocketHandler extends TextWebSocketHandler {

    // 结构：classId -> 属于该班级的 Session 集合
    private static final Map<Long, CopyOnWriteArraySet<WebSocketSession>> classSessionMap = new ConcurrentHashMap<>();

    @Resource
    private ClassChatMessageService classChatMessageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long classId = (Long) session.getAttributes().get("classId");
        if (classId != null) {
            classSessionMap.computeIfAbsent(classId, k -> new CopyOnWriteArraySet<>()).add(session);
            session.getAttributes().put("lastActiveTime", System.currentTimeMillis());
            log.info("WebSocket 连接建立: 班级{}, 用户{}, Session: {}", classId, session.getAttributes().get("userName"), session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.getAttributes().put("lastActiveTime", System.currentTimeMillis());
        
        String payload = message.getPayload();
        try {
            JSONObject msgObj = JSONUtil.parseObj(payload);
            String type = msgObj.getStr("type");
            
            if ("ping".equals(type)) {
                // 回复 pong
                JSONObject pongObj = new JSONObject();
                pongObj.set("type", "pong");
                session.sendMessage(new TextMessage(pongObj.toString()));
                return;
            }
            
            if ("chat".equals(type)) {
                String content = msgObj.getStr("content");
                Integer messageType = msgObj.getInt("messageType", 0); // 默认为文本消息
                String imageUrl = msgObj.getStr("imageUrl");
                Long classId = (Long) session.getAttributes().get("classId");
                Long senderId = (Long) session.getAttributes().get("userId");
                String userName = (String) session.getAttributes().get("userName");
                String userAvatar = (String) session.getAttributes().get("userAvatar");

                // 1. 持久化到 MySQL
                ClassChatMessage chatMessage = new ClassChatMessage();
                chatMessage.setClassId(classId);
                chatMessage.setSenderId(senderId);
                chatMessage.setContent(content);
                chatMessage.setMessageType(messageType);
                chatMessage.setImageUrl(imageUrl);
                chatMessage.setCreateTime(new Date());
                classChatMessageService.save(chatMessage);

                // 2. 组装广播消息体 (含用户信息)
                JSONObject broadcastMsg = new JSONObject();
                broadcastMsg.set("type", "chat");
                broadcastMsg.set("id", String.valueOf(chatMessage.getId()));
                broadcastMsg.set("classId", String.valueOf(classId));
                broadcastMsg.set("senderId", String.valueOf(senderId));
                broadcastMsg.set("userName", userName);
                broadcastMsg.set("userAvatar", userAvatar);
                broadcastMsg.set("content", content);
                broadcastMsg.set("messageType", messageType);
                broadcastMsg.set("imageUrl", imageUrl);
                broadcastMsg.set("createTime", chatMessage.getCreateTime().getTime());

                // 3. 发布到 Redis Pub/Sub (供所有微服务节点消费)
                stringRedisTemplate.convertAndSend("class_chat_channel:" + classId, broadcastMsg.toString());
            }
        } catch (Exception e) {
            log.error("处理WebSocket文本消息异常", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long classId = (Long) session.getAttributes().get("classId");
        if (classId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = classSessionMap.get(classId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    classSessionMap.remove(classId);
                }
            }
        }
        log.info("WebSocket 连接关闭: 班级{}, Session: {}", classId, session.getId());
    }

    /**
     * 接收 Redis 的广播消息并向本地匹配此班级的 Session 下发
     */
    public void broadcastToLocal(Long classId, String messageJson) {
        CopyOnWriteArraySet<WebSocketSession> sessions = classSessionMap.get(classId);
        if (sessions != null && !sessions.isEmpty()) {
            TextMessage textMessage = new TextMessage(messageJson);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("广播消息失败", e);
                    }
                }
            }
        }
    }
    
    /**
     * 清理所有超时的连接
     */
    public void cleanTimeoutSessions() {
        long now = System.currentTimeMillis();
        // 90 秒超时
        long timeoutMillis = 90000;
        for (Map.Entry<Long, CopyOnWriteArraySet<WebSocketSession>> entry : classSessionMap.entrySet()) {
            for (WebSocketSession session : entry.getValue()) {
                Long lastActiveTime = (Long) session.getAttributes().get("lastActiveTime");
                if (lastActiveTime != null && (now - lastActiveTime > timeoutMillis)) {
                    log.warn("清理超时WebSocket连接: {}", session.getId());
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (IOException e) {
                        log.error("关闭超时长连接异常", e);
                    }
                }
            }
        }
    }
}
