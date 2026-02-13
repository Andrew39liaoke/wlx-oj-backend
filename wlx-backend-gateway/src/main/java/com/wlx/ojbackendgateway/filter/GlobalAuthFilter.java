package com.wlx.ojbackendgateway.filter;

import com.wlx.ojbackendcommon.utils.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import jakarta.annotation.Resource;
import com.wlx.ojbackendgateway.config.WhiteList;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import static com.wlx.ojbackendcommon.constant.RedisConstant.TOKEN_PREFIX;

@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Resource
    private WhiteList whiteList;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String path = serverHttpRequest.getURI().getPath();
        String[] matchers = whiteList.getRequestMatchers();
        if (matchers != null) {
            for (String pattern : matchers) {
                String patternPath = pattern;
                if (patternPath.contains(" ")) {
                    patternPath = patternPath.substring(patternPath.indexOf(' ') + 1);
                }
                if (antPathMatcher.match(patternPath, path)) {
                    // 白名单路径直接放行，设置一个标记让后续过滤器跳过
                    exchange.getAttributes().put("WHITELIST_PASSED", true);
                    return chain.filter(exchange);
                }
            }
        }
        // 判断路径中是否包含 inner，只允许内部调用
        if (antPathMatcher.match("/**/inner/**", path)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            DataBuffer dataBuffer = dataBufferFactory.wrap("无权限".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer));
        }
        // 统一权限校验，通过 JWT 获取登录用户信息（与 JwtFilter 行为一致）
        // 按照 JwtFilter 的实现：若 header 为空则交由下游处理（返回 true / 放行）
        String token = serverHttpRequest.getHeaders().getFirst(JwtUtil.HEADER);
        if (token == null) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            DataBuffer dataBuffer = dataBufferFactory.wrap("未登录".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer));
        }
        // 若携带 token，则校验合法性和过期
        try {
            Claims claims = JwtUtil.getClaimsByToken(token);
            if (JwtUtil.isTokenExpired(claims.getExpiration())) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                DataBufferFactory dataBufferFactory = response.bufferFactory();
                DataBuffer dataBuffer = dataBufferFactory.wrap("token已过期，请重新登录".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(dataBuffer));
            }
            // 继续校验：确保 Redis 中存在该 token（用户未登出 / token 未被服务端删除）
            String redisKey = TOKEN_PREFIX + token;
            Boolean exists = stringRedisTemplate.hasKey(redisKey);
            if (exists == null || !exists) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                DataBufferFactory dataBufferFactory = response.bufferFactory();
                DataBuffer dataBuffer = dataBufferFactory.wrap("token 无效或已登出".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(dataBuffer));
            }
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            DataBuffer dataBuffer = dataBufferFactory.wrap("token无效".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer));
        }
        return chain.filter(exchange);
    }

    /**
     * 优先级提到最高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
