package com.wlx.ojbackendaiservice.ws;

import cn.hutool.core.util.StrUtil;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.annotation.Resource;
import java.nio.charset.Charset;
import java.util.Map;

@Component
@Slf4j
public class ClassChatHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserFeignClient userFeignClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String query = request.getURI().getQuery();
        
        String token = null;
        String classIdStr = null;
        
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if ("token".equals(pair[0])) {
                        token = pair[1];
                    } else if ("classId".equals(pair[0])) {
                        classIdStr = pair[1];
                    }
                }
            }
        }

        if (StrUtil.isBlank(token) || StrUtil.isBlank(classIdStr)) {
            log.warn("WebSocket握手失败，缺失token或classId信息: {}", request.getURI());
            return false;
        }

        try {
            Long classId = Long.parseLong(classIdStr);
            // 通过Feign调用鉴权，得到当前用户
            User loginUser = userFeignClient.getLoginUser(token);
            if (loginUser == null) {
                log.warn("WebSocket握手失败，token无效");
                return false;
            }

            // 将用户及班级信息放入WebSocketSession的attributes中
            attributes.put("userId", loginUser.getId());
            attributes.put("userName", loginUser.getUserName());
            attributes.put("userAvatar", loginUser.getUserAvatar());
            attributes.put("classId", classId);
            attributes.put("token", token);
            return true;
        } catch (Exception e) {
            log.error("WebSocket握手异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
