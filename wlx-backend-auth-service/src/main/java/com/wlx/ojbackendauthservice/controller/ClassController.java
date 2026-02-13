package com.wlx.ojbackendauthservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQuestionQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassStudentQueryRequest;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.ClassVO;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendauthservice.service.ClassService;
import com.wlx.ojbackendauthservice.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/class")
public class ClassController {
    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    /**
     * 根据邀请码加入班级
     *
     * @param invitationCode 邀请码
     * @param request 请求
     * @return 是否加入成功
     */
    @PostMapping("/join")
    public ResponseEntity<Boolean> joinClass(@RequestParam("invitationCode") String invitationCode,
                                              HttpServletRequest request) {
        // 从请求头获取token并解析用户ID
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        boolean result = classService.joinClass(invitationCode, user.getId());
        return Result.success(result);
    }

    /**
     * 退出班级
     *
     * @param classIds 班级ID列表
     * @param request 请求
     * @return 是否退出成功
     */
    @PostMapping("/quit")
    public ResponseEntity<Boolean> quitClass(@RequestParam List<Long> classIds,
                                               HttpServletRequest request) {
        // 从请求头获取token并解析用户ID
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        boolean result = classService.quitClass(classIds, user.getId());
        return Result.success(result);
    }

    /**
     * 分页获取班级题目列表
     *
     * @param classQuestionQueryRequest 查询请求
     * @return 分页题目VO
     */
    @PostMapping("/question/page")
    public ResponseEntity<Page<QuestionVO>> getClassQuestionPage(@RequestBody ClassQuestionQueryRequest classQuestionQueryRequest) {
        Page<QuestionVO> page = classService.getClassQuestionPage(classQuestionQueryRequest);
        return Result.success(page);
    }

    /**
     * 获取班级题目的用户提交信息
     *
     * @param classId 班级ID
     * @param request 请求
     * @return 题目提交信息列表
     */
    @GetMapping("/question/submitInfo")
    public ResponseEntity<List<Map<String, Object>>> getClassQuestionSubmitInfo(@RequestParam("classId") Long classId,
                                                                                 HttpServletRequest request) {
        // 从请求头获取token并解析用户ID
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        List<Map<String, Object>> result = classService.getClassQuestionSubmitInfo(classId, user.getId());
        return Result.success(result);
    }

    /**
     * 分页获取班级列表
     *
     * @param classQueryRequest 查询请求
     * @return 分页班级VO
     */
    @PostMapping("/page")
    public ResponseEntity<Page<ClassVO>> getClassPage(@RequestBody ClassQueryRequest classQueryRequest) {
        Page<ClassVO> page = classService.getClassPage(classQueryRequest);
        return Result.success(page);
    }

    /**
     * 增加班级
     *
     * @param name 班级名称
     * @param request 请求
     * @return 新创建的班级ID
     */
    @PostMapping("/add")
    public ResponseEntity<Long> addClass(@RequestParam("name") String name,
                                          HttpServletRequest request) {
        // 从请求头获取token并解析用户ID
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username));
        if (user == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        Long classId = classService.addClass(name, user.getId());
        return Result.success(classId);
    }

    /**
     * 分页获取班级的学生列表
     *
     * @param classStudentQueryRequest 查询请求
     * @return 分页学生
     */
    @GetMapping("/student/page")
    public ResponseEntity<Page<User>> getClassStudentPage(@RequestBody ClassStudentQueryRequest classStudentQueryRequest) {
        Page<User> page = classService.getClassStudentPage(classStudentQueryRequest);
        return Result.success(page);
    }

    /**
     * 为班级添加题目
     *
     * @param classId 班级ID
     * @param questionAddRequest 题目添加请求
     * @return 是否添加成功
     */
    @PostMapping("/problem/add")
    public ResponseEntity<Boolean> addClassProblems(@RequestParam("classId") Long classId,
                                                     @RequestBody QuestionAddRequest questionAddRequest) {
        boolean result = classService.addClassProblems(classId, questionAddRequest);
        return Result.success(result);
    }

    /**
     * 批量删除班级题目
     *
     * @param classId 班级ID
     * @param problemIds 题目ID列表
     * @return 是否删除成功
     */
    @PostMapping("/problem/delete")
    public ResponseEntity<Boolean> deleteClassProblems(@RequestParam("classId") Long classId,
                                                        @RequestParam("problemIds") List<Long> problemIds) {
        boolean result = classService.deleteClassProblems(classId, problemIds);
        return Result.success(result);
    }

}
