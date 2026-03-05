package com.wlx.ojbackendquestionservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.entity.UserRecommendation;
import com.wlx.ojbackendmodel.model.vo.RecommendationVO;

public interface RecommendationService extends IService<UserRecommendation> {

    /**
     * 获取题目推荐列表
     *
     * @param userId   用户ID
     * @param pageSize 每页数量
     * @param current  当前页
     * @return 推荐题目列表
     */
    Page<RecommendationVO> getQuestionRecommendations(Long userId, long pageSize, long current);

    /**
     * 获取帖子推荐列表
     *
     * @param userId   用户ID
     * @param pageSize 每页数量
     * @param current  当前页
     * @return 推荐帖子列表
     */
    Page<RecommendationVO> getPostRecommendations(Long userId, long pageSize, long current);

    /**
     * 触发全量推荐计算（定时任务调用）
     */
    void computeAllRecommendations();

    /**
     * 刷新指定用户的推荐结果
     *
     * @param userId 用户ID
     */
    void refreshUserRecommendations(Long userId);
}
