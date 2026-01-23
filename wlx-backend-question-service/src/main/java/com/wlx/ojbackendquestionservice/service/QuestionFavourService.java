package com.wlx.ojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.QuestionFavour;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;

import java.util.List;

public interface QuestionFavourService extends IService<QuestionFavour> {

    /**
     * 收藏题目
     */
    boolean addFavour(Long questionId, Long userId);

    /**
     * 取消收藏
     */
    boolean removeFavour(Long questionId, Long userId);

    /**
     * 根据用户 id 分页获取收藏的题目（返回 QuestionVO 分页）
     */
    Page<QuestionVO> getFavourQuestionVOPage(QuestionQueryRequest req);

    /**
     * 批量查询用户对题目的收藏状态
     * @param questionIds 题目ID列表
     * @param userId 用户ID
     * @return 用户已收藏的题目ID列表
     */
    List<Long> getFavourStatus(List<Long> questionIds, Long userId);
}
