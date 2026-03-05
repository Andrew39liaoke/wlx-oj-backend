package com.wlx.ojbackendserviceclient.service;


import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionFavour;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.QuestionThumb;
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
     * 根据多个用户ID和题目ID批量查询提交记录
     *
     * @param userIds 用户ID列表
     * @param questionId 题目ID
     * @return 提交记录列表
     */
    @GetMapping("/question_submit/list/byUsersAndQuestion")
    List<QuestionSubmit> listQuestionSubmitsByUserIdsAndQuestionId(@RequestParam("userIds") Collection<Long> userIds,
                                                                   @RequestParam("questionId") Long questionId);


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

    /**
     * 增加题目提交数
     *
     * @param questionId 题目ID
     * @return 是否成功
     */
    @PostMapping("/increment_submit")
    boolean incrementSubmitNum(@RequestParam("questionId") Long questionId);

    /**
     * 增加题目通过数
     *
     * @param questionId 题目ID
     * @return 是否成功
     */
    @PostMapping("/increment_accepted")
    boolean incrementAcceptedNum(@RequestParam("questionId") Long questionId);

    /**
     * 获取所有题目提交记录
     *
     * @return 题目提交记录列表
     */
    @GetMapping("/question_submit/listAll")
    List<QuestionSubmit> listAllQuestionSubmits();

    /**
     * 获取所有题目点赞记录
     *
     * @return 题目点赞记录列表
     */
    @GetMapping("/question_thumb/listAll")
    List<QuestionThumb> listAllQuestionThumbs();

    /**
     * 获取所有题目收藏记录
     *
     * @return 题目收藏记录列表
     */
    @GetMapping("/question_favour/listAll")
    List<QuestionFavour> listAllQuestionFavours();

    /**
     * 获取所有未删除题目列表
     *
     * @return 题目列表
     */
    @GetMapping("/list/all")
    List<Question> listAllQuestions();

}
