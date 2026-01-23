package com.wlx.ojbackendserviceclient.service;

import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * 用户服务
 */
@FeignClient(name = "wlx-backend-auth-service", path = "/api/auth/inner")
public interface  UserFeignClient {

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @GetMapping("/get/id")
    User getById(@RequestParam("userId") long userId);

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @GetMapping("/get/ids")
    List<User> listByIds(@RequestParam("idList") Collection<Long> idList);
    @GetMapping("/getPermissionByUsername")
    List<String> getPermissionByUsername(@RequestParam("username") String username);
    @GetMapping("/getRoleNamesByUsername")
    List<String> getRoleNamesByUsername(@RequestParam("username") String username);
    /**
     * 获取当前登录用户
     *
     * @param token
     * @return
     */
    @GetMapping("/getLoginUser")
    User getLoginUser(@RequestParam("token") String token);



    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

}
