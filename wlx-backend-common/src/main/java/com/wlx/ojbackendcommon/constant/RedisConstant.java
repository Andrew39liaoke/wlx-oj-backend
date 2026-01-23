package com.wlx.ojbackendcommon.constant;

/**
 * Redis 相关常量
 */
public interface RedisConstant {

    /**
     * token 在 Redis 中的前缀（例如 Key = token: + tokenValue）
     */
    String TOKEN_PREFIX = "token:";

    String PERMISSION_PREFIX = "permission:";

    String Role_Permission = "role_permission";
}
