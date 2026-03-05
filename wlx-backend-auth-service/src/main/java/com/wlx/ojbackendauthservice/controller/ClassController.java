package com.wlx.ojbackendauthservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendauthservice.service.ClassKnowledgeService;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.Class.*;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.ClassKnowledgeVO;
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

    @Resource
    private ClassKnowledgeService classKnowledgeService;

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
     * 教师或管理员移除班级中的学生
     *
     * @param removeRequest 移除请求DTO
     * @param request 请求
     * @return 是否成功移除
     */
    @PostMapping("/student/remove")
    public ResponseEntity<Boolean> removeClassStudent(@RequestBody ClassStudentRemoveRequest removeRequest,
                                                      HttpServletRequest request) {
        if (removeRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        // 从请求头获取 token 并解析判断登录
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        boolean result = classService.removeClassStudent(removeRequest);
        if (!result) {
            throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "移除学生失败，可能是越权操作或班级不存在");
        }
        return Result.success(true);
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
     `1     * 获取班级题目的用户提交信息
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
     * 获取指定班级所有人对某个题目的提交统计数据（通过率、提交数）
     *
     * @param classId 班级ID
     * @param questionId 题目ID
     * @param request 请求
     * @return 包含提交数和通过数的 Map
     */
    @GetMapping("/question/submit/stats")
    public ResponseEntity<Map<String, Object>> getClassQuestionSubmitStats(@RequestParam("classId") Long classId,
                                                                           @RequestParam("questionId") Long questionId,
                                                                           HttpServletRequest request) {
        // 从请求头获取token并解析判断登录
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

        Map<String, Object> stats = classService.getClassQuestionSubmitStats(classId, questionId);
        return Result.success(stats);
    }

    /**
     * 获取指定班级所有人对某个题目的详细提交状态（包含谁完成了、未完成、提交次数）
     *
     * @param classId 班级ID
     * @param questionId 题目ID
     * @param request 请求
     * @return 包含学生名称、完成状态、提交次数的详情列表
     */
    @GetMapping("/question/submit/detail")
    public ResponseEntity<List<Map<String, Object>>> getClassQuestionSubmitDetail(@RequestParam("classId") Long classId,
                                                                                  @RequestParam("questionId") Long questionId,
                                                                                  HttpServletRequest request) {
        // 从请求头获取token并解析判断登录
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

        List<Map<String, Object>> details = classService.getClassQuestionSubmitDetail(classId, questionId);
        return Result.success(details);
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
     * 分页获取教师创建的班级列表
     *
     * @param classTeacherQueryRequest 查询请求（可包含班级名称等）
     * @return 分页班级VO
     */
    @PostMapping("/my/create/page")
    public ResponseEntity<Page<ClassVO>> getMyCreatedClassPage(@RequestBody ClassTeacherQueryRequest classTeacherQueryRequest,
                                                               HttpServletRequest request) {
        // 如果未传入教师ID，则将当前登录教师的ID赋给查询条件
        if (classTeacherQueryRequest.getTeacherId() == null) {
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
            classTeacherQueryRequest.setTeacherId(user.getId());
        }

        Page<ClassVO> page = classService.getTeacherClasses(classTeacherQueryRequest);
        return Result.success(page);
    }

    /**
     * 分页获取某个学生所在的所有班级列表
     *
     * @param classQueryRequest 查询请求（包含学生ID和班级名称）
     * @return 分页班级VO
     */
    @PostMapping("/student/classes")
    public ResponseEntity<Page<ClassVO>> getStudentClasses(@RequestBody ClassQueryRequest classQueryRequest) {
        Page<ClassVO> page = classService.getStudentClasses(classQueryRequest);
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
    @PostMapping("/student/page")
    public ResponseEntity<Page<User>> getClassStudentPage(@RequestBody ClassStudentQueryRequest classStudentQueryRequest) {
        Page<User> page = classService.getClassStudentPage(classStudentQueryRequest);
        return Result.success(page);
    }

    /** `
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

    /**
     * 批量删除班级
     *
     * @param classIds 班级ID列表
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public ResponseEntity<Boolean> deleteClass(@RequestParam("classIds") List<Long> classIds,
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
        boolean result = classService.deleteClasses(classIds, user.getId());
        return Result.success(result);
    }

    /**
     * 获取班级统计图表数据
     *
     * @param classId 班级ID
     * @param request 请求
     * @return 聚合统计数据
     */
    @GetMapping("/stats/charts")
    public ResponseEntity<Map<String, Object>> getClassStatsCharts(@RequestParam("classId") Long classId,
                                                                   HttpServletRequest request) {
        // 从请求头获取token并解析判断登录
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

        Map<String, Object> stats = classService.getClassStatsCharts(classId);
        return Result.success(stats);
    }

    /**
     * 添加班级知识库文件
     *
     * @param classId 班级ID
     * @param file 知识库文件
     * @param request 请求
     * @return 是否添加成功
     */
    @PostMapping("/knowledge/add")
    public ResponseEntity<Boolean> addClassKnowledge(@RequestParam("classId") Long classId,
                                                     @RequestPart(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                                     HttpServletRequest request) {
        // 从请求头获取token并解析判断登录
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

        boolean result = classKnowledgeService.addClassKnowledge(file, classId, user.getId());
        return Result.success(result);
    }

    /**
     * 删除班级知识库文件
     *
     * @param id 知识库ID
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/knowledge/delete")
    public ResponseEntity<Boolean> deleteClassKnowledge(@RequestParam("id") Long id,
                                                        HttpServletRequest request) {
        // 从请求头获取token并解析判断登录
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

        boolean result = classKnowledgeService.deleteClassKnowledge(id, user.getId());
        return Result.success(result);
    }

    /**
     * 获取班级知识库列表
     *
     * @param classId 班级ID
     * @return 知识库列表
     */
    @GetMapping("/knowledge/list")
    public ResponseEntity<List<ClassKnowledgeVO>> listClassKnowledge(@RequestParam("classId") Long classId) {
        List<ClassKnowledgeVO> list = classKnowledgeService.listClassKnowledge(classId);
        return Result.success(list);
    }

}
