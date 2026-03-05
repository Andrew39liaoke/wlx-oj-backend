package com.wlx.ojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.recommend.RecommendQueryRequest;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.RecommendationVO;
import com.wlx.ojbackendquestionservice.service.RecommendationService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 推荐 API 控制器
 */
@RestController
@RequestMapping("/recommend")
@Slf4j
public class RecommendationController {

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 获取题目推荐列表
     */
    @PostMapping("/question")
    public ResponseEntity<Page<RecommendationVO>> getQuestionRecommendations(
            @RequestBody RecommendQueryRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getLoginUserId(httpRequest);
        long pageSize = request.getPageSize();
        long current = request.getCurrent();
        Page<RecommendationVO> result = recommendationService.getQuestionRecommendations(userId, pageSize, current);
        return Result.success(result);
    }

    /**
     * 获取帖子推荐列表
     */
    @PostMapping("/post")
    public ResponseEntity<Page<RecommendationVO>> getPostRecommendations(
            @RequestBody RecommendQueryRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getLoginUserId(httpRequest);
        long pageSize = request.getPageSize();
        long current = request.getCurrent();
        Page<RecommendationVO> result = recommendationService.getPostRecommendations(userId, pageSize, current);
        return Result.success(result);
    }

    /**
     * 手动刷新推荐
     */
    @PostMapping("/refresh")
    public ResponseEntity<Boolean> refreshRecommendations(HttpServletRequest httpRequest) {
        Long userId = getLoginUserId(httpRequest);
        recommendationService.refreshUserRecommendations(userId);
        return Result.success(true);
    }

    /**
     * 从请求中获取登录用户ID
     */
    private Long getLoginUserId(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(JwtUtil.HEADER);
        User loginUser = userFeignClient.getLoginUser(token);
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser.getId();
    }
}
