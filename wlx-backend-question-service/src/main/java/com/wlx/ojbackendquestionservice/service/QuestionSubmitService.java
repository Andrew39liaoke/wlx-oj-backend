package com.wlx.ojbackendquestionservice.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.wlx.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.QuestionSubmitVO;

import java.util.List;

public interface QuestionSubmitService extends IService<QuestionSubmit> {

    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest 题目提交信息
     * @param loginUser
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser);
    /**
     * 获取指定年月的签到日历（打卡日期列表）
     * @param year 年份
     * @param month 月份
     * @param userId 用户 ID
     * @return 该月打卡的具体日期列表（如 1, 2, 15, 27）
     */
    List<Integer> getSubmitCalendar(int year, int month, Long userId);
}
