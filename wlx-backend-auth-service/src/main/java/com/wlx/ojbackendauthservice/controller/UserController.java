package com.wlx.ojbackendauthservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.user.*;
import com.wlx.ojbackendmodel.model.entity.Role;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.enums.UserRoleEnum;
import com.wlx.ojbackendmodel.model.token.JwtToken;
import com.wlx.ojbackendmodel.model.vo.LoginUserVO;
import com.wlx.ojbackendmodel.model.vo.UserVO;
import com.wlx.ojbackendauthservice.service.RoleService;
import com.wlx.ojbackendauthservice.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;
    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        String userName = userRegisterRequest.getUserName();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userName, userPassword, checkPassword);
        return Result.success(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserVO> login(@RequestBody @Validated UserLoginRequest userLoginRequest, HttpServletResponse response) {
        String username = userLoginRequest.getUserName();
        String password = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(username, password);
        // 获取用户角色信息并生成包含用户类型的token
        List<String> roles = userService.getRoleNamesByUsername(username);
        String userType = userService.getUserTypeByRoles(roles);
        String token = JwtUtil.generateToken(username, userType);
        // 将用户信息存储到Shiro Subject中，让后续授权检查能获取到用户信息
        try {
            JwtToken jwtToken = new JwtToken(token);
            SecurityUtils.getSubject().login(jwtToken);
        } catch (Exception e) {
            return Result.error(ResopnseCodeEnum.LOGIN_FAILED);
        }
        loginUserVO.setToken(token);
        return Result.success(loginUserVO);
    }
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public ResponseEntity<LoginUserVO> getLoginUser(HttpServletRequest request) {
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        ThrowUtils.throwIf(user == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        String type = JwtUtil.getUserTypeFromToken(token);
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);
        loginUserVO.setUserRole(type);
        System.out.println("loginUserVO = " + loginUserVO);
        return Result.success(loginUserVO);
    }


    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        Role role = new Role();
        role.setValue(userAddRequest.getUserRole());
        role.setName(UserRoleEnum.getEnumByValue(userAddRequest.getUserRole()).getText());
        boolean save = roleService.save(role);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return Result.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public ResponseEntity<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return Result.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public ResponseEntity<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 如果未提供 userRole，则不修改角色
        if (userUpdateRequest.getUserRole() != null) {
            UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userUpdateRequest.getUserRole());
            // 如果枚举存在，则保存/更新角色；枚举不存在则跳过角色修改（不抛异常）
            if (userRoleEnum != null) {
                Role role = new Role();
                role.setValue(userUpdateRequest.getUserRole());
                role.setName(userRoleEnum.getText());
                roleService.save(role);
            }
        }
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return Result.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public ResponseEntity<User> getUserById(long id) {
        if (id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        return Result.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public ResponseEntity<UserVO> getUserVOById(long id, HttpServletRequest request) {
        ResponseEntity<User> responseEntity = getUserById(id);
        User user = responseEntity.getData();
        return Result.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public ResponseEntity<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                     HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return Result.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public ResponseEntity<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                         HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResopnseCodeEnum.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return Result.success(userVOPage);
    }


    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public ResponseEntity<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                                HttpServletRequest request) {
        if (userUpdateMyRequest == null || userUpdateMyRequest.getUserId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(userUpdateMyRequest.getUserId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return Result.success(true);
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        // 从 Redis 中删除 token（通过 service 层）
        userService.userLogout(request);
        // 退出登录（Shiro 相关）
        SecurityUtils.getSubject().logout();
        return Result.success("退出成功");
    }

}
