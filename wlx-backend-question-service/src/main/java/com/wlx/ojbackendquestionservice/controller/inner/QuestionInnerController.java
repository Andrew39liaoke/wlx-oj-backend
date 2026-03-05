package com.wlx.ojbackendquestionservice.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionFavour;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.QuestionThumb;
import com.wlx.ojbackendquestionservice.service.QuestionFavourService;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import com.wlx.ojbackendquestionservice.service.QuestionSubmitService;
import com.wlx.ojbackendquestionservice.service.QuestionThumbService;
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

    @Resource
    private QuestionThumbService questionThumbService;

    @Resource
    private QuestionFavourService questionFavourService;

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

    @GetMapping("/get/answer")
    @Override
    public String getQuestionAnswer(@RequestParam("questionId") Long questionId) {
        return questionService.getAnswerByQuestionId(questionId);
    }

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

    @GetMapping("/question_submit/list/byUsersAndQuestion")
    @Override
    public List<QuestionSubmit> listQuestionSubmitsByUserIdsAndQuestionId(@RequestParam("userIds") Collection<Long> userIds,
                                                                          @RequestParam("questionId") Long questionId) {
        if (userIds == null || userIds.isEmpty() || questionId == null) {
            return List.of();
        }
        return questionSubmitService.list(
                new LambdaQueryWrapper<QuestionSubmit>()
                        .in(QuestionSubmit::getUserId, userIds)
                        .eq(QuestionSubmit::getQuestionId, questionId)
        );
    }

    @GetMapping("/list/byIds")
    @Override
    public List<Question> listQuestionsByIds(@RequestParam("questionIds") Collection<Long> questionIds) {
        return questionService.listByIds(questionIds);
    }

    @PostMapping("/add")
    @Override
    public Long addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {
        Question question = new Question();
        question.setTitle(questionAddRequest.getTitle());
        question.setContent(questionAddRequest.getContent());
        question.setAnswer(questionAddRequest.getAnswer());
        if (questionAddRequest.getTags() != null) {
            question.setTags(JSONUtil.toJsonStr(questionAddRequest.getTags()));
        }
        if (questionAddRequest.getJudgeCase() != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(questionAddRequest.getJudgeCase()));
        }
        if (questionAddRequest.getJudgeConfig() != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(questionAddRequest.getJudgeConfig()));
        }
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        question.setThumbNum(0);
        question.setFavourNum(0);
        question.setUserId(1L);
        questionService.save(question);
        return question.getId();
    }

    @PostMapping("/increment_submit")
    @Override
    public boolean incrementSubmitNum(@RequestParam("questionId") Long questionId) {
        return questionService.incrementSubmitNum(questionId);
    }

    @PostMapping("/increment_accepted")
    @Override
    public boolean incrementAcceptedNum(@RequestParam("questionId") Long questionId) {
        return questionService.incrementAcceptedNum(questionId);
    }

    /**
     * 获取所有题目提交记录
     */
    @GetMapping("/question_submit/listAll")
    @Override
    public List<QuestionSubmit> listAllQuestionSubmits() {
        return questionSubmitService.list();
    }

    /**
     * 获取所有题目点赞记录
     */
    @GetMapping("/question_thumb/listAll")
    @Override
    public List<QuestionThumb> listAllQuestionThumbs() {
        return questionThumbService.list();
    }

    /**
     * 获取所有题目收藏记录
     */
    @GetMapping("/question_favour/listAll")
    @Override
    public List<QuestionFavour> listAllQuestionFavours() {
        return questionFavourService.list();
    }

    /**
     * 获取所有未删除题目列表
     */
    @GetMapping("/list/all")
    @Override
    public List<Question> listAllQuestions() {
        return questionService.list();
    }
}
