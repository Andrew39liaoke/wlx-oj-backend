package com.wlx.ojbackendauthservice.realm;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.token.JwtToken;
import com.wlx.ojbackendauthservice.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

import static com.wlx.ojbackendcommon.constant.RedisConstant.*;
import static com.wlx.ojbackendcommon.constant.UserConstant.ADMIN_ROLE;

@Component
@Slf4j
public class AccountRealm extends AuthorizingRealm {
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Gson GSON = new Gson();

    /**
     * 多重写一个support
     * 标识这个Realm是专门用来验证JwtToken
     * 不负责验证其他的token（UsernamePasswordToken）
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 认证
     * <p>
     * 只是获取比对的信息
     * 认证的逻辑还是按照shrio底层认证逻辑完成
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        String jwt = (String) authenticationToken.getCredentials();
        // 获取jwt中关于用户名
        String username = JwtUtil.getClaimsByToken(jwt).getSubject();
        // 查询用户（通过内部用户服务 Feign 客户端）
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName,username));
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.BAD_REQUEST);
        }
        if (user.getIsDelete() == 1) {
            throw new BusinessException(ResopnseCodeEnum.BAD_REQUEST, "用户被锁定");
        }
        Claims claims = JwtUtil.getClaimsByToken(jwt);
        if (JwtUtil.isTokenExpired(claims.getExpiration())) {
            throw new BusinessException(ResopnseCodeEnum.BAD_REQUEST, "token过期，请重新登录");
        }
        log.info("认证成功");
        // 计算 token 剩余秒数（用于 Redis key 过期）
        long seconds = 0L;
        if (claims != null && claims.getExpiration() != null) {
            seconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        }
        // 认证成功后，将 token 存入 Redis（key = TOKEN_PREFIX + token），value 为 user
        String redisKey = TOKEN_PREFIX + jwt;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (exists == null || !exists) {
            try {
                String userJson = GSON.toJson(user);
                if (seconds > 0) {
                    stringRedisTemplate.opsForValue().set(redisKey, userJson, seconds, TimeUnit.SECONDS);
                } else {
                    stringRedisTemplate.opsForValue().set(redisKey, userJson);
                }
            } catch (Exception e) {
                log.warn("写入 Redis token(User JSON) 失败: {}", e.getMessage());
            }
        }
        Boolean role_permission = stringRedisTemplate.hasKey(Role_Permission);
        if (role_permission == null || !role_permission){
            // 缓存用户角色对应的权限（按角色 key 存放），如果 key 已存在则不修改
            cachePermissionsPerRole(username, seconds);
        }
        return new SimpleAuthenticationInfo(user, jwt, getName());
    }

    /**
     * 授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        // 获取当前登录用户信息
        User user = (User) principalCollection.getPrimaryPrincipal();
        log.info("【授权信息】当前登录用户: {}", user); // 确保User类重写了toString()方法
        log.info("【授权信息】当前登录用户名: {}", user.getUserName());
        // 创建授权信息对象
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        // 获取用户角色列表（通过内部用户服务 Feign 客户端）
        List<String> roleNames = userService.getRoleNamesByUsername(user.getUserName());
        authorizationInfo.addRoles(roleNames);
        // 获取用户权限列表
        List<String> permissions = userService.getPermissionByUsername(user.getUserName());
        authorizationInfo.addStringPermissions(permissions);
        log.info("【授权信息】最终封装的角色集合: {}", authorizationInfo.getRoles());
        log.info("【授权信息】最终封装的权限集合: {}", authorizationInfo.getStringPermissions());
        return authorizationInfo;
    }

    /**
     * 将用户所属角色的权限缓存到 Redis
     * 根据用户类型（admin或user）缓存相应的权限
     * 如果对应角色的权限key已存在则不修改
     *
     * @param username 登录用户
     */
    private void cachePermissionsPerRole(String username, long seconds) {
        try {
            List<String> roleNames = userService.getRoleNamesByUsername(username);
            // 根据角色集合判断用户类型
            String userType = userService.getUserTypeByRoles(roleNames);
            // 只缓存管理员的权限到按角色的 Redis key，其他用户类型不写入
            if (!ADMIN_ROLE.equalsIgnoreCase(userType)) {
                return;
            }
            stringRedisTemplate.opsForValue().set(Role_Permission,"1");
            String rolePermissionKey = PERMISSION_PREFIX + userType;
            Boolean rolePermExists = stringRedisTemplate.hasKey(rolePermissionKey);
            if (rolePermExists != null && rolePermExists) {
                return; // 权限已缓存，跳过
            }
            try {
                List<String> permissions = userService.getPermissionByUsername(username);
                if (permissions != null && !permissions.isEmpty()) {
                    String permValue = String.join(",", permissions);
                    if (seconds > 0) {
                        stringRedisTemplate.opsForValue().set(rolePermissionKey, permValue, seconds, TimeUnit.SECONDS);
                    } else {
                        stringRedisTemplate.opsForValue().set(rolePermissionKey, permValue);
                    }
                    log.info("成功缓存{}角色权限到Redis，key: {}", userType, rolePermissionKey);
                }
            } catch (Exception e) {
                log.warn("获取用户权限失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("写入 Redis permission（按角色）失败: {}", e.getMessage());
        }
    }
}
