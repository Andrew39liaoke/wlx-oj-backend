package com.wlx.ojbackendquestionservice.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import com.wlx.ojbackendquestionservice.service.QuestionSubmitService;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    /**
     * 根据题目ID获取解答过程
     *
     * @param questionId 题目ID
     * @return 解答过程，如果不存在则返回 null
     */
    @GetMapping("/get/answer")
    @Override
    public String getQuestionAnswer(@RequestParam("questionId") Long questionId) {
        return questionService.getAnswerByQuestionId(questionId);
    }

    /**
     * 根据用户ID和题目ID列表批量查询提交记录
     *
     * @param userId 用户ID
     * @param questionIds 题目ID列表
     * @return 提交记录列表
     */
    @GetMapping("/question_submit/list/byUserAndQuestions")
    @Override
    public List<QuestionSubmit> listQuestionSubmitsByUserAndQuestions(@RequestParam("userId") Long userId,
                                                                       @RequestParam("questionIds") Collection<Long> questionIds) {
        return questionSubmitService.list(
                new LambdaQueryWrapper<QuestionSubmit>()
                        .eq(QuestionSubmit::getUserId, userId)
                        .in(QuestionSubmit::getQuestionId, questionIds)
        );
    }

    /**
     * 根据题目ID列表批量查询题目信息
     *
     * @param questionIds 题目ID列表
     * @return 题目列表
     */
    @GetMapping("/list/byIds")
    @Override
    public List<Question> listQuestionsByIds(@RequestParam("questionIds") Collection<Long> questionIds) {
        return questionService.listByIds(questionIds);
    }

    /**
     * 创建题目
     *
     * @param questionAddRequest 题目创建请求
     * @return 创建的题目ID
     */
    @PostMapping("/add")
    @Override
    public Long addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {
        Question question = new Question();
        question.setTitle(questionAddRequest.getTitle());
        question.setContent(questionAddRequest.getContent());
        question.setAnswer(questionAddRequest.getAnswer());
        // 设置标签
        if (questionAddRequest.getTags() != null) {
            question.setTags(JSONUtil.toJsonStr(questionAddRequest.getTags()));
        }
        // 设置判题用例
        if (questionAddRequest.getJudgeCase() != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(questionAddRequest.getJudgeCase()));
        }
        // 设置判题配置
        if (questionAddRequest.getJudgeConfig() != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(questionAddRequest.getJudgeConfig()));
        }
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        question.setThumbNum(0);
        question.setFavourNum(0);
        question.setUserId(1L); // 默认用户ID，实际应该从请求中获取
        questionService.save(question);
        return question.getId();
    }

}
