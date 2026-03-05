package com.wlx.ojbackendauthservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.wlx.ojbackendauthservice.mapper.UserMapper;
import com.wlx.ojbackendauthservice.mapper.StudentClassMapper;
import com.wlx.ojbackendauthservice.mapper.ClassMapper;
import com.wlx.ojbackendauthservice.mapper.ClassProblemMapper;
import com.wlx.ojbackendauthservice.mapper.QuestionMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private StudentClassMapper studentClassMapper;

    @Resource
    private ClassMapper classMapper;

    @Resource
    private ClassProblemMapper classProblemMapper;

    @Resource
    private QuestionMapper questionMapper;

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
        if (CollectionUtils.isNotEmpty(roles)) {
            if (roles.contains("admin")) {
                return "admin";
            }
            if (roles.contains("teacher")) {
                return "teacher";
            }
        }
        return "student";
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
                Integer roleId = RoleEnum.STUDENT.getValue();
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
        userRole.setRoleId((long) RoleEnum.STUDENT.getValue());
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

        // 查询用户角色，判断是否被封禁
        List<String> roleNames = getRoleNamesByUsername(userName);
        if (roleNames != null && roleNames.contains("ban")) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR, "您的账号已被封禁");
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

        // 动态查询用户角色，避免Token保存过期角色导致信息不同步
        if (user != null) {
            try {
                List<String> roleNames = getRoleNamesByUsername(user.getUserName());
                String userType = getUserTypeByRoles(roleNames);
                user.setRole(userType);
            } catch (Exception e) {
                log.warn("实时查询用户类型失败,回退到Token提取: {}", e.getMessage());
                try {
                    String userType = JwtUtil.getUserTypeFromToken(token);
                    user.setRole(userType);
                } catch (Exception ex) {
                    user.setRole(null);
                }
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
        System.out.println("redisKey = " + redisKey);
        // 删除Redis中的token
        Boolean deleteResult = stringRedisTemplate.delete(redisKey);
        System.out.println("deleteResult = " + deleteResult);
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
        // 主动查询用户角色（role 字段不在数据库中，需从 user_role 关联表获取）
        try {
            List<String> roleNames = getRoleNamesByUsername(user.getUserName());
            String userType = getUserTypeByRoles(roleNames);
            userVO.setUserRole(userType);
        } catch (Exception e) {
            log.warn("查询用户角色失败: userId={}, {}", user.getId(), e.getMessage());
            userVO.setUserRole("student");
        }
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

    /**
     * 根据学生ID查询其所在的所有班级
     *
     * @param studentId 学生ID
     * @return 班级信息列表，包含班级ID和班级名称
     */
    @Override
    public List<Map<String, Object>> getStudentClasses(Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (studentId == null) {
            return result;
        }
        // 查询学生加入的所有班级关联记录
        List<StudentClass> studentClasses = studentClassMapper.selectList(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getStudentId, studentId)
        );
        if (CollectionUtils.isNotEmpty(studentClasses)) {
            // 获取所有班级ID
            List<Long> classIds = studentClasses.stream()
                    .map(StudentClass::getClassId)
                    .collect(Collectors.toList());
            // 批量查询班级信息
            List<com.wlx.ojbackendmodel.model.entity.Class> classList = classMapper.selectBatchIds(classIds);
            if (CollectionUtils.isNotEmpty(classList)) {
                // 构建班级ID到班级对象的映射
                Map<Long, com.wlx.ojbackendmodel.model.entity.Class> classMap = classList.stream()
                        .collect(Collectors.toMap(com.wlx.ojbackendmodel.model.entity.Class::getId, c -> c));
                // 按原始顺序构建返回结果
                for (StudentClass sc : studentClasses) {
                    com.wlx.ojbackendmodel.model.entity.Class classInfo = classMap.get(sc.getClassId());
                    if (classInfo != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("班级id", classInfo.getId());
                        map.put("班级名称", classInfo.getName());
                        result.add(map);
                    }
                }
            }
        }
        log.info("根据学生ID查询班级信息: studentId={}, 结果={}", studentId, result);
        return result;
    }

    /**
     * 根据学生ID查询其所在班级的所有题目
     *
     * @param studentId 学生ID
     * @return 题目信息列表，包含题目ID和标题
     */
    @Override
    public List<Map<String, Object>> getStudentClassProblems(Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (studentId == null) {
            return result;
        }
        // 先查询学生所在的所有班级
        List<Map<String, Object>> classes = getStudentClasses(studentId);
        if (CollectionUtils.isEmpty(classes)) {
            log.info("学生未加入任何班级: studentId={}", studentId);
            return result;
        }
        // 收集所有班级ID
        List<Long> classIds = classes.stream()
                .map(c -> (Long) c.get("班级id"))
                .collect(Collectors.toList());
        // 查询这些班级中的所有题目关联
        List<ClassProblem> classProblems = classProblemMapper.selectList(
                Wrappers.<ClassProblem>lambdaQuery().in(ClassProblem::getClassId, classIds)
        );
        if (CollectionUtils.isEmpty(classProblems)) {
            log.info("班级中没有题目: studentId={}, classIds={}", studentId, classIds);
            return result;
        }
        // 收集所有题目ID
        List<Long> problemIds = classProblems.stream()
                .map(ClassProblem::getProblemId)
                .collect(Collectors.toList());
        // 批量查询题目信息
        List<Question> questions = questionMapper.selectBatchIds(problemIds);
        if (CollectionUtils.isNotEmpty(questions)) {
            // 构建题目ID到题目对象的映射
            Map<Long, Question> questionMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            // 按去重后的顺序构建返回结果
            for (ClassProblem cp : classProblems) {
                Question question = questionMap.get(cp.getProblemId());
                if (question != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("题目id", question.getId());
                    map.put("题目标题", question.getTitle());
                    result.add(map);
                }
            }
        }
        log.info("根据学生ID查询班级题目: studentId={}, 结果数量={}", studentId, result.size());
        return result;
    }

    /**
     * 根据班级ID获取该班级的所有题目ID
     *
     * @param classId 班级ID
     * @return 题目ID列表
     */
    @Override
    public List<Long> getClassProblemIds(Long classId) {
        if (classId == null) {
            return new ArrayList<>();
        }
        List<ClassProblem> classProblems = classProblemMapper.selectList(
                Wrappers.<ClassProblem>lambdaQuery().eq(ClassProblem::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(classProblems)) {
            return new ArrayList<>();
        }
        return classProblems.stream()
                .map(ClassProblem::getProblemId)
                .collect(Collectors.toList());
    }

    /**
     * 更新用户后同步更新Redis缓存
     *
     * @param userId 用户ID
     * @param token  用户的token
     * @return 是否同步成功
     */
    @Override
    public boolean syncUserCache(Long userId, String token) {
        if (userId == null || StringUtils.isBlank(token)) {
            return false;
        }
        // 构建Redis key
        String redisKey = TOKEN_PREFIX + token;
        // 检查Redis中是否存在该key
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (exists == null || !exists) {
            // Redis中没有缓存，无需同步
            return true;
        }
        try {
            // 从数据库获取最新的用户信息
            User latestUser = this.getById(userId);
            if (latestUser == null) {
                log.warn("同步用户缓存失败：用户不存在, userId={}", userId);
                return false;
            }
            // 将最新用户信息写入Redis
            String userJson = GSON.toJson(latestUser);
            // 获取token剩余过期时间
            Long ttl = stringRedisTemplate.getExpire(redisKey);
            if (ttl != null && ttl > 0) {
                stringRedisTemplate.opsForValue().set(redisKey, userJson, ttl, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(redisKey, userJson);
            }
            log.info("用户缓存已同步更新: userId={}, redisKey={}", userId, redisKey);
            return true;
        } catch (Exception e) {
            log.error("同步用户缓存失败: {}", e.getMessage());
            return false;
        }
    }
}
