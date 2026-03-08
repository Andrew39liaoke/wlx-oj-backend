package com.wlx.ojbackendaiservice.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import jakarta.annotation.Resource;

@Configuration
@EnableWebSocket
public class ClassChatWebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ClassChatWebSocketHandler classChatWebSocketHandler;

    @Resource
    private LiveWebSocketHandler liveWebSocketHandler;

    @Resource
    private ClassChatHandshakeInterceptor classChatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 班级群聊 WebSocket
        registry.addHandler(classChatWebSocketHandler, "/api/ai/chat/ws")
                .addInterceptors(classChatHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 直播间 WebSocket
        registry.addHandler(liveWebSocketHandler, "/api/ai/live/ws")
                .addInterceptors(classChatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
