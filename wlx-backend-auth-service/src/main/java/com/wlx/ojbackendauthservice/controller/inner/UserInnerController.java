package com.wlx.ojbackendauthservice.controller.inner;

import com.wlx.ojbackendauthservice.mapper.ClassMapper;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import com.wlx.ojbackendauthservice.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
@Slf4j
public class UserInnerController implements UserFeignClient {

    @Resource
    private UserService userService;

    @Resource
    private ClassMapper classMapper;

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/get/id")
    public User getById(@RequestParam("userId") long userId) {
        return userService.getById(userId);
    }

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @Override
    @GetMapping("/get/ids")
    public List<User> listByIds(@RequestParam("idList") Collection<Long> idList) {
        return userService.listByIds(idList);
    }

    @Override
    @GetMapping("/getPermissionByUsername")
    public List<String> getPermissionByUsername(@RequestParam("username") String username) {
        return userService.getPermissionByUsername(username);
    }

    @Override
    @GetMapping("/getRoleNamesByUsername")
    public List<String> getRoleNamesByUsername(@RequestParam("username") String username) {
        return userService.getRoleNamesByUsername(username);
    }

    @Override
    @GetMapping("/getLoginUser")
    public User getLoginUser(@RequestParam("token") String token) {
        return userService.getLoginUser(token);
    }

    @Override
    @PostMapping("/updateAvatar")
    public boolean updateUserAvatar(@RequestParam("userId") Long userId, @RequestParam("avatarUrl") String avatarUrl) {
        User user = new User();
        user.setId(userId);
        user.setUserAvatar(avatarUrl);
        return userService.updateById(user);
    }

    /**
     * 根据学生ID查询其所在的所有班级
     * @param studentId 学生ID
     * @return 班级信息列表
     */
    @Override
    @GetMapping("/getStudentClasses")
    public List<Map<String, Object>> getStudentClasses(@RequestParam("studentId") Long studentId) {
        return userService.getStudentClasses(studentId);
    }

    /**
     * 根据学生ID查询其所在班级的所有题目
     * @param studentId 学生ID
     * @return 题目信息列表
     */
    @Override
    @GetMapping("/getStudentClassProblems")
    public List<Map<String, Object>> getStudentClassProblems(@RequestParam("studentId") Long studentId) {
        return userService.getStudentClassProblems(studentId);
    }

    /**
     * 更新班级的文件信息ID
     *
     * @param classId 班级ID
     * @param fileInfoId 文件信息ID
     * @return 是否更新成功
     */
    @Override
    @PostMapping("/class/updateFileInfoId")
    public boolean updateClassFileInfoId(@RequestParam("classId") Long classId, @RequestParam("fileInfoId") Long fileInfoId) {
        log.info("更新班级文件信息，classId: {}, fileInfoId: {}", classId, fileInfoId);

        com.wlx.ojbackendmodel.model.entity.Class classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            log.warn("班级不存在，classId: {}", classId);
            return false;
        }

        classEntity.setFileInfoId(fileInfoId);
        return classMapper.updateById(classEntity) > 0;
    }
}
