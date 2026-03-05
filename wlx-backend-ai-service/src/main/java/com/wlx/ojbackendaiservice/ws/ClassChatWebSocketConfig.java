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
    private ClassChatHandshakeInterceptor classChatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(classChatWebSocketHandler, "/api/ai/chat/ws")
                .addInterceptors(classChatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
