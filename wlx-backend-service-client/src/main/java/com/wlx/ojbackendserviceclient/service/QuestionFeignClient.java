package com.wlx.ojbackendserviceclient.service;


import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "wlx-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {

    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    /**
     * 根据题目ID获取解答过程
     *
     * @param questionId 题目ID
     * @return 解答过程，如果不存在则返回 null
     */
    @GetMapping("/get/answer")
    String getQuestionAnswer(@RequestParam("questionId") Long questionId);

    /**
     * 根据用户ID和题目ID列表批量查询提交记录
     *
     * @param userId 用户ID
     * @param questionIds 题目ID列表
     * @return 提交记录列表
     */
    @GetMapping("/question_submit/list/byUserAndQuestions")
    List<QuestionSubmit> listQuestionSubmitsByUserAndQuestions(@RequestParam("userId") Long userId,
                                                                @RequestParam("questionIds") Collection<Long> questionIds);

    /**
     * 根据题目ID列表批量查询题目信息
     *
     * @param questionIds 题目ID列表
     * @return 题目列表
     */
    @GetMapping("/list/byIds")
    List<Question> listQuestionsByIds(@RequestParam("questionIds") Collection<Long> questionIds);

    /**
     * 创建题目
     *
     * @param questionAddRequest 题目创建请求
     * @return 创建的题目ID
     */
    @PostMapping("/add")
    Long addQuestion(@RequestBody QuestionAddRequest questionAddRequest);

}
