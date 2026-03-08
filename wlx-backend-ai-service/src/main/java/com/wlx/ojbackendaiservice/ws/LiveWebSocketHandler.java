package com.wlx.ojbackendaiservice.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 直播间 WebSocket Handler
 * 负责：观众上下线通知、直播聊天、直播状态广播、观众计数
 */
@Component
@Slf4j
public class LiveWebSocketHandler extends TextWebSocketHandler {

    // 结构：roomId -> 该直播间的 Session 集合
    private static final Map<String, CopyOnWriteArraySet<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();

    // 直播间观众计数
    private static final Map<String, AtomicInteger> viewerCountMap = new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long classId = (Long) session.getAttributes().get("classId");
        String roomId = (String) session.getAttributes().get("roomId");
        String userName = (String) session.getAttributes().get("userName");

        if (roomId == null) {
            // 统一使用 String 形式的 classId 作为房间标识，确保与前端/Redis监听器一致
            roomId = String.valueOf(classId);
            session.getAttributes().put("roomId", roomId);
        }

        roomSessionMap.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        int count = viewerCountMap.computeIfAbsent(roomId, k -> new AtomicInteger(0)).incrementAndGet();
        session.getAttributes().put("lastActiveTime", System.currentTimeMillis());

        log.info("直播WebSocket连接建立: 房间{}, 用户{}, 当前观众数: {}", roomId, userName, count);

        // 广播观众加入通知
        JSONObject joinMsg = new JSONObject();
        joinMsg.set("type", "viewer_join");
        joinMsg.set("userName", userName);
        joinMsg.set("viewerCount", count);
        stringRedisTemplate.convertAndSend("live_room_channel:" + roomId, joinMsg.toString());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        session.getAttributes().put("lastActiveTime", System.currentTimeMillis());

        String payload = message.getPayload();
        try {
            JSONObject msgObj = JSONUtil.parseObj(payload);
            String type = msgObj.getStr("type");

            if ("ping".equals(type)) {
                JSONObject pongObj = new JSONObject();
                pongObj.set("type", "pong");
                synchronized (session) {
                    session.sendMessage(new TextMessage(pongObj.toString()));
                }
                return;
            }

            String roomId = (String) session.getAttributes().get("roomId");

            if ("chat".equals(type)) {
                String content = msgObj.getStr("content");
                Long senderId = (Long) session.getAttributes().get("userId");
                String userName = (String) session.getAttributes().get("userName");
                String userAvatar = (String) session.getAttributes().get("userAvatar");
                String userRole = (String) session.getAttributes().get("userRole");

                // 【对齐交流大厅技术】组装全量消息对象，但不进行数据库持久化（符合用户“关播即逝”要求）
                JSONObject broadcastMsg = new JSONObject();
                broadcastMsg.set("type", "chat");
                broadcastMsg.set("senderId", String.valueOf(senderId));
                broadcastMsg.set("userName", userName);
                broadcastMsg.set("userAvatar", userAvatar);
                broadcastMsg.set("content", content);
                broadcastMsg.set("color", msgObj.getStr("color", "#ffffff"));
                broadcastMsg.set("size", msgObj.getInt("size", 20));
                broadcastMsg.set("role", "teacher".equals(userRole) || "admin".equals(userRole) ? 0 : 1);
                broadcastMsg.set("timestamp", System.currentTimeMillis());

                // 仅仅通过 Redis 频道广播（满足分布式实时消费），不执行任何 save 请求
                stringRedisTemplate.convertAndSend("live_room_channel:" + roomId, broadcastMsg.toString());
            }

            if ("live_status".equals(type)) {
                // ... 原有逻辑保持
                String status = msgObj.getStr("status");
                JSONObject statusMsg = new JSONObject();
                statusMsg.set("type", "live_status");
                statusMsg.set("status", status);
                statusMsg.set("timestamp", System.currentTimeMillis());
                stringRedisTemplate.convertAndSend("live_room_channel:" + roomId, statusMsg.toString());
            }

        } catch (Exception e) {
            log.error("处理直播WebSocket消息异常", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        String userName = (String) session.getAttributes().get("userName");

        if (roomId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = roomSessionMap.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessionMap.remove(roomId);
                }
            }

            int count = 0;
            AtomicInteger counter = viewerCountMap.get(roomId);
            if (counter != null) {
                count = counter.decrementAndGet();
                if (count <= 0) {
                    viewerCountMap.remove(roomId);
                    count = 0;
                }
            }

            // 1. 广播观众离开详细通知 (用于系统提示)
            JSONObject leaveMsg = new JSONObject();
            leaveMsg.set("type", "viewer_leave");
            leaveMsg.set("userName", userName);
            leaveMsg.set("viewerCount", count);
            stringRedisTemplate.convertAndSend("live_room_channel:" + roomId, leaveMsg.toString());

            // 2. 广播纯人数通知 (确保组件更新)
            JSONObject countMsg = new JSONObject();
            countMsg.set("type", "viewer_count");
            countMsg.set("count", count);
            stringRedisTemplate.convertAndSend("live_room_channel:" + roomId, countMsg.toString());
        }
        log.info("直播WebSocket连接关闭: 房间{}, 用户{}", roomId, userName);
    }

    /**
     * 接收 Redis 广播消息并下发给本地该房间的所有 Session
     */
    public void broadcastToLocal(String roomId, String messageJson) {
        CopyOnWriteArraySet<WebSocketSession> sessions = roomSessionMap.get(roomId);
        if (sessions != null && !sessions.isEmpty()) {
            TextMessage textMessage = new TextMessage(messageJson);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        // 同步发送，避免多线程并发写入同一个 session 导致 TEXT_PARTIAL_WRITING
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        log.error("直播广播消息失败", e);
                    } catch (IllegalStateException e) {
                        log.warn("WebSocket session 状态异常，跳过: {}", e.getMessage());
                    }
                }
            }
        }
    }
    /**
     * 清理所有超时的连接 (对齐交流大厅技术规范)
     */
    public void cleanTimeoutSessions() {
            long now = System.currentTimeMillis();
            // 90 秒超时
            long timeoutMillis = 90000;
            for (Map.Entry<String, CopyOnWriteArraySet<WebSocketSession>> entry : roomSessionMap.entrySet()) {
                for (WebSocketSession session : entry.getValue()) {
                    Long lastActiveTime = (Long) session.getAttributes().get("lastActiveTime");
                    if (lastActiveTime != null && (now - lastActiveTime > timeoutMillis)) {
                        log.warn("清理超时直播WebSocket连接: {}, roomId: {}", session.getId(), entry.getKey());
                        try {
                            session.close(CloseStatus.SESSION_NOT_RELIABLE);
                        } catch (IOException e) {
                            log.error("关闭超时直播长连接异常", e);
                        }
                    }
                }
            }
        }
}
