package com.wlx.ojbackendauthservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.wlx.ojbackendauthservice.mapper.UserMapper;
import com.wlx.ojbackendauthservice.service.*;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.constant.CommonConstant;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendcommon.utils.SqlUtils;
import com.wlx.ojbackendmodel.model.dto.user.UserQueryRequest;
import com.wlx.ojbackendmodel.model.entity.*;
import com.wlx.ojbackendmodel.model.enums.RoleEnum;
import com.wlx.ojbackendmodel.model.vo.LoginUserVO;
import com.wlx.ojbackendmodel.model.vo.UserVO;
import com.wlx.ojbackendmodel.model.wx.wxUser;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.wlx.ojbackendcommon.constant.RedisConstant.TOKEN_PREFIX;
import static com.wlx.ojbackendcommon.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "wlx";

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private RolePermissionService rolePermissionService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private RoleService roleService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Gson GSON = new Gson();

    public List<String> getPermissionByUsername(String username) {
        List<String> permissionNames = new ArrayList<>();
        User user = super.getOne(Wrappers.<User>lambdaQuery().eq(User::getUserName, username), true);
        if (null != user) {
            List<UserRole> userRoles = userRoleService.list(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, user.getId()));
            if (CollectionUtils.isNotEmpty(userRoles)) {
                List<Long> roleIds = new ArrayList<>();
                userRoles.stream().forEach(userRole -> {
                    roleIds.add(userRole.getRoleId());
                });
                List<RolePermission> rolePermissions = rolePermissionService.list(Wrappers.<RolePermission>lambdaQuery().in(RolePermission::getRoleId, roleIds));
                if (CollectionUtils.isNotEmpty(rolePermissions)) {
                    List<Long> permissionIds = new ArrayList<>();
                    rolePermissions.stream().forEach(rolePermission -> {
                        permissionIds.add(rolePermission.getPermissionId());
                    });
                    List<Permission> permissions = permissionService.list(Wrappers.<Permission>lambdaQuery().in(Permission::getId, permissionIds));
                    permissions.stream().forEach(permission -> {
                        permissionNames.add(permission.getUrl());
                    });
                }
            }
        }
        return permissionNames;
    }

    /**
     * 根据用户名获取用户角色列表
     *
     * @param username 用户名
     * @return 角色名称列表
     */
    public List<String> getRoleNamesByUsername(String username) {
        List<String> roleNames = new ArrayList<>();
        User user = super.getOne(Wrappers.<User>lambdaQuery().eq(User::getUserName, username), true);
        if (null != user) {
            List<UserRole> userRoles = userRoleService.list(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, user.getId()));
            if (CollectionUtils.isNotEmpty(userRoles)) {
                List<Long> roleIds = new ArrayList<>();
                userRoles.stream().forEach(userRole -> {
                    roleIds.add(userRole.getRoleId());
                });
                List<Role> roles = roleService.list(Wrappers.<Role>lambdaQuery().in(Role::getId, roleIds));
                roles.stream().forEach(role -> {
                    roleNames.add(role.getValue());
                });
            }
        }
        return roleNames;
    }

    /**
     * 根据角色集合判断用户类型
     *
     * @param roles 角色名称列表
     * @return 如果包含admin返回"admin"，否则返回"user"
     */
    public String getUserTypeByRoles(List<String> roles) {
        if (CollectionUtils.isNotEmpty(roles) && roles.contains("admin")) {
            return "admin";
        }
        return "user";
    }

    /**
     * 根据微信open_id查询用户，如果不存在则创建默认用户
     *
     * @return 用户对象
     */
    public User findOrCreateByWxOpenId(wxUser wxUser) {
        String OpenId = wxUser == null ? null : wxUser.getOpenid();
        User user = super.getOne(Wrappers.<User>lambdaQuery().eq(User::getMpOpenId, OpenId), true);
        if (user == null) {
            // 用户不存在，创建新用户
            user = new User();
            // 生成唯一的随机用户名（8位UUID前缀）
            String randomUsername;
            do {
                randomUsername = "wx_" + UUID.randomUUID().toString().substring(0, 8);
            } while (isUsernameExists(randomUsername)); // 确保用户名唯一
            user.setUserName(randomUsername);
            user.setUnionId(wxUser.getUnionid());
            user.setMpOpenId(OpenId);
            user.setNickName(wxUser.getNickname());
            user.setUserAvatar(wxUser.getHeadimgurl());
            user.setUserPassword(""); // 微信用户无需密码
            // 保存用户
            super.save(user);
            // 保存用户-角色关联，默认角色为 RoleEnum.USER
            try {
                Integer roleId = RoleEnum.USER.getValue();
                UserRole userRole = new UserRole();
                if (user.getId() != null) {
                    userRole.setUserId(user.getId());
                }
                userRole.setRoleId(roleId.longValue());
                userRoleService.save(userRole);
            } catch (Exception ignore) {
                // 如果角色保存失败不影响用户创建，记录或忽略错误
            }
        }
        return user;
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return true-存在，false-不存在
     */
    private boolean isUsernameExists(String username) {
        return super.count(Wrappers.<User>lambdaQuery()
                .eq(User::getUserName, username)) > 0;
    }

    @Override
    public long userRegister(String userName, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() < 4) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 账户不能重复 - 确保user_name字段有唯一索引以提高查询性能
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "账号重复");
        }
        // 2. 加密 - 使用更安全的加密算法
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserName(userName);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId((long) RoleEnum.USER.getValue());
        userRoleService.save(userRole);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userName, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() < 4) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        queryWrapper.eq("user_password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "用户不存在或密码错误");
        }
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param token
     * @return
     */
    @Override
    public User getLoginUser(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        // 构建Redis key
        String redisKey = TOKEN_PREFIX + token;
        // 首先判断Redis中是否存在该key
        User user = null;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (exists != null && exists) {
            // 如果存在，从Redis中获取用户信息并转换为实体类
            try {
                String userJson = stringRedisTemplate.opsForValue().get(redisKey);
                user = GSON.fromJson(userJson, User.class);
            } catch (Exception e) {
                log.warn("从Redis解析用户信息失败: {}", e.getMessage());
            }
        }

        // 从 token 中解析用户类型并设置到 user 对象中
        if (user != null) {
            try {
                String userType = JwtUtil.getUserTypeFromToken(token);
                user.setRole(userType);
            } catch (Exception e) {
                log.warn("从token解析用户类型失败: {}", e.getMessage());
                user.setRole(null);
            }
        }

        return user;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 从请求头获取JWT token
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        // 构建Redis key
        String redisKey = TOKEN_PREFIX + token;
        // 删除Redis中的token
        Boolean deleteResult = stringRedisTemplate.delete(redisKey);
        if (deleteResult != null && deleteResult) {
            log.info("用户token已从Redis中删除: {}", redisKey);
        }
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        System.out.println("user = " + user);
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String nickName = userQueryRequest.getNickName();
        String userProfile = userQueryRequest.getUserProfile();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "union_id", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mp_open_id", mpOpenId);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(nickName), "nick_name", nickName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
