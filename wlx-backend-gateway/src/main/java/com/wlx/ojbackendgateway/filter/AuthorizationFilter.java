package com.wlx.ojbackendgateway.filter;

import com.wlx.ojbackendcommon.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.wlx.ojbackendcommon.constant.RedisConstant.PERMISSION_PREFIX;
import static com.wlx.ojbackendcommon.constant.UserConstant.ADMIN_ROLE;

@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查是否已经被GlobalAuthFilter的白名单放行
        Boolean whitelistPassed = exchange.getAttribute("WHITELIST_PASSED");
        if (Boolean.TRUE.equals(whitelistPassed)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        System.out.println("AuthorizationFilter path = " + path);
        // 从 header 读取 token（与 GlobalAuthFilter 保持一致）
        String token = request.getHeaders().getFirst(JwtUtil.HEADER);
        // 从token中直接获取用户类型，避免Feign调用
        String userType = JwtUtil.getUserTypeFromToken(token);
        if (ADMIN_ROLE.equalsIgnoreCase(userType)) {
            return chain.filter(exchange);
        }
        // 对于非admin用户，检查是否访问admin专属路径
        if (userType != null && !userType.isEmpty()) {
            // 从 redis 中取出 admin 的权限模式，判断当前请求路径是否属于 admin 专属路径
            String adminRoleKey = PERMISSION_PREFIX + ADMIN_ROLE;
            String adminPermValue = stringRedisTemplate.hasKey(adminRoleKey) ? stringRedisTemplate.opsForValue().get(adminRoleKey) : null;

            // 规范化请求路径一次
            String sanitizedPath = path.replaceAll("^/api/[^/]+", ""); // 移除 /api/xxx 前缀
            sanitizedPath = sanitizedPath.replaceAll("//+", "/");
            sanitizedPath = sanitizedPath.replaceAll(":([a-zA-Z_][a-zA-Z0-9_]*)", "{$1}");

            boolean pathBelongsToAdmin = false;
            if (StringUtils.isNotBlank(adminPermValue)) {
                String[] adminPerms = adminPermValue.split(",");
                for (String permPattern : adminPerms) {
                    permPattern = permPattern == null ? "" : permPattern.trim();
                    if (StringUtils.isBlank(permPattern)) {
                        continue;
                    }
                    String normalizedPattern = permPattern.startsWith("/") ? permPattern : "/" + permPattern;
                    if (antPathMatcher.match(normalizedPattern, sanitizedPath)) {
                        pathBelongsToAdmin = true;
                        break;
                    }
                }
            }

            // 如果该路径属于 admin 专属路径，但当前用户不是 admin，则直接拦截
            if (pathBelongsToAdmin && !ADMIN_ROLE.equalsIgnoreCase(userType)) {
                // 直接返回 403，非 admin 无法访问 admin 专属路径
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                byte[] bytes = "没有访问权限".getBytes(StandardCharsets.UTF_8);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
            }

            // 用户不是 admin 且路径不属于 admin 专属时，直接放行
            return chain.filter(exchange);
        }
        // 如果没有有效的用户类型或token，返回 403
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        byte[] bytes = "没有访问权限".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().
                wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // 运行在 GlobalAuthFilter 之后（GlobalAuthFilter order = 0）
        return 1;
    }
}


