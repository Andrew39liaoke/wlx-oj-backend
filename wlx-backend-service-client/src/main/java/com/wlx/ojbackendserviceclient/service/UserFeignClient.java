package com.wlx.ojbackendserviceclient.service;

import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * 更新用户头像
     *
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 是否更新成功
     */
    @PostMapping("/updateAvatar")
    boolean updateUserAvatar(@RequestParam("userId") Long userId, @RequestParam("avatarUrl") String avatarUrl);

    /**
     * 根据学生ID查询其所在的所有班级
     *
     * @param studentId 学生ID
     * @return 班级信息列表，包含班级ID和班级名称
     */
    @GetMapping("/getStudentClasses")
    List<Map<String, Object>> getStudentClasses(@RequestParam("studentId") Long studentId);

    /**
     * 根据学生ID查询其所在班级的所有题目
     *
     * @param studentId 学生ID
     * @return 题目信息列表，包含题目ID和标题
     */
    @GetMapping("/getStudentClassProblems")
    List<Map<String, Object>> getStudentClassProblems(@RequestParam("studentId") Long studentId);


    /**
     * 更新班级的文件信息ID
     *
     * @param classId 班级ID
     * @param fileInfoId 文件信息ID
     * @return 是否更新成功
     */
    @PostMapping("/class/updateFileInfoId")
    boolean updateClassFileInfoId(@RequestParam("classId") Long classId, @RequestParam("fileInfoId") Long fileInfoId);

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
