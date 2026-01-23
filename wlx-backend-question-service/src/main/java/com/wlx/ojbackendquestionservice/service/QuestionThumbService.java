package com.wlx.ojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.QuestionThumb;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;

import java.util.List;

public interface QuestionThumbService extends IService<QuestionThumb> {

    /**
     * 点赞题目
     */
    boolean addThumb(Long questionId, Long userId);

    /**
     * 取消点赞
     */
    boolean removeThumb(Long questionId, Long userId);

    /**
     * 根据用户 id 分页获取点赞的题目（返回 QuestionVO 分页）
     */
    Page<QuestionVO> getThumbQuestionVOPage(QuestionQueryRequest req);

    /**
     * 批量查询用户对题目的点赞状态
     * @param questionIds 题目ID列表
     * @param userId 用户ID
     * @return 用户已点赞的题目ID列表
     */
    List<Long> getThumbStatus(List<Long> questionIds, Long userId);
}
