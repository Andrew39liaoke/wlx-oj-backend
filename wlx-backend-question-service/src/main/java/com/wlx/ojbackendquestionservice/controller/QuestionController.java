package com.wlx.ojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.question.*;
import com.wlx.ojbackendquestionservice.service.QuestionThumbService;
import com.wlx.ojbackendquestionservice.service.QuestionFavourService;
import com.wlx.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.wlx.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.QuestionSubmitVO;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import com.wlx.ojbackendquestionservice.service.QuestionSubmitService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
 
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionThumbService questionThumbService;

    @Resource
    private QuestionFavourService questionFavourService;

    private final static Gson GSON = new Gson();

    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return Result.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public ResponseEntity<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return Result.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public ResponseEntity<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return Result.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public ResponseEntity<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        // 不是本人或管理员，不能直接获取所有信息
        if (!question.getUserId().equals(loginUser.getId()) && !"admin".equals(loginUser.getRole())) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        return Result.success(question);
    }

    /**
     * 根据 id 获取（脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public ResponseEntity<QuestionVO> getQuestionVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        return Result.success(questionService.getQuestionVO(question));
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public ResponseEntity<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResopnseCodeEnum.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return Result.success(questionService.getQuestionVOPage(questionPage));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public ResponseEntity<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                   HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResopnseCodeEnum.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return Result.success(questionService.getQuestionVOPage(questionPage));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public ResponseEntity<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                             HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return Result.success(questionPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public ResponseEntity<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !"admin".equals(loginUser.getRole())) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return Result.success(result);
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/question_submit/do")
    public ResponseEntity<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                                 HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        // 登录才能提交
        final User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return Result.success(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    public ResponseEntity<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                           HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        // 返回脱敏信息
        return Result.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * 批量查询用户对题目的点赞和收藏状态
     *
     * @param request 请求体，包含题目ID列表和用户ID
     * @return 用户操作状态响应
     */
    @PostMapping("/action/status")
    public ResponseEntity<QuestionActionStatusResponse> getActionStatus(@RequestBody QuestionActionStatusRequest request) {
        if (request == null || request.getQuestionIds() == null || request.getUserId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }

        // 批量查询点赞状态
        List<Long> thumbedQuestions = questionThumbService.getThumbStatus(request.getQuestionIds(), request.getUserId());

        // 批量查询收藏状态
        List<Long> favouredQuestions = questionFavourService.getFavourStatus(request.getQuestionIds(), request.getUserId());

        // 构建响应
        QuestionActionStatusResponse response = new QuestionActionStatusResponse();
        response.setThumbedQuestions(thumbedQuestions);
        response.setFavouredQuestions(favouredQuestions);

        return Result.success(response);
    }

}
