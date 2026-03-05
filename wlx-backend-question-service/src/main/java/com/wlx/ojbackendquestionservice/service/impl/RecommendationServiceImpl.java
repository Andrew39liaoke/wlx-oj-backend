package com.wlx.ojbackendquestionservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.UserRecommendation;
import com.wlx.ojbackendmodel.model.vo.RecommendationVO;
import com.wlx.ojbackendquestionservice.mapper.UserRecommendationMapper;
import com.wlx.ojbackendquestionservice.service.CollaborativeFilteringEngine;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import com.wlx.ojbackendquestionservice.service.RecommendationService;
import com.wlx.ojbackendserviceclient.service.PostFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 */
@Service
@Slf4j
public class RecommendationServiceImpl extends ServiceImpl<UserRecommendationMapper, UserRecommendation>
        implements RecommendationService {

    @Resource
    private CollaborativeFilteringEngine cfEngine;

    @Resource
    private QuestionService questionService;

    @Resource
    private PostFeignClient postFeignClient;


    // 协同过滤参数
    private static final int TOP_K = 20;          // 相似用户数
    private static final int TOP_N = 20;          // 推荐结果数
    private static final int MIN_COMMON_ITEMS_Q = 3; // 题目最少共同交互
    private static final int MIN_COMMON_ITEMS_P = 2; // 帖子最少共同交互

    // 推荐类型常量
    private static final int TYPE_QUESTION = 1;
    private static final int TYPE_POST = 2;

    @Override
    public Page<RecommendationVO> getQuestionRecommendations(Long userId, long pageSize, long current) {
        // 1. 先从数据库查询已缓存的推荐结果
        LambdaQueryWrapper<UserRecommendation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRecommendation::getUserId, userId)
                .eq(UserRecommendation::getRecommendType, TYPE_QUESTION)
                .orderByDesc(UserRecommendation::getScore);

        Page<UserRecommendation> recommendPage = page(new Page<>(current, pageSize), queryWrapper);
        List<UserRecommendation> records = recommendPage.getRecords();

        // 2. 如果没有推荐结果，实时计算一次
        if (records == null || records.isEmpty()) {
            log.info("用户 {} 无缓存题目推荐，实时计算", userId);
            refreshUserRecommendations(userId);
            // 重新查询
            recommendPage = page(new Page<>(current, pageSize), queryWrapper);
            records = recommendPage.getRecords();
        }

        // 3. 填充题目详细信息
        List<RecommendationVO> voList = fillQuestionDetails(records);

        // 4. 构建分页结果
        Page<RecommendationVO> resultPage = new Page<>(current, pageSize, recommendPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Page<RecommendationVO> getPostRecommendations(Long userId, long pageSize, long current) {
        LambdaQueryWrapper<UserRecommendation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRecommendation::getUserId, userId)
                .eq(UserRecommendation::getRecommendType, TYPE_POST)
                .orderByDesc(UserRecommendation::getScore);

        Page<UserRecommendation> recommendPage = page(new Page<>(current, pageSize), queryWrapper);
        List<UserRecommendation> records = recommendPage.getRecords();

        if (records == null || records.isEmpty()) {
            log.info("用户 {} 无缓存帖子推荐，实时计算", userId);
            refreshUserRecommendations(userId);
            recommendPage = page(new Page<>(current, pageSize), queryWrapper);
            records = recommendPage.getRecords();
        }

        List<RecommendationVO> voList = fillPostDetails(records);

        Page<RecommendationVO> resultPage = new Page<>(current, pageSize, recommendPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public void computeAllRecommendations() {
        log.info("===== 开始全量推荐计算 =====");
        long startTime = System.currentTimeMillis();

        try {
            // 构建评分矩阵
            Map<Long, Map<Long, Double>> questionMatrix = cfEngine.buildQuestionRatingMatrix();
            Map<Long, Map<Long, Double>> postMatrix = cfEngine.buildPostRatingMatrix();

            // 收集所有用户ID
            Set<Long> allUserIds = new HashSet<>();
            allUserIds.addAll(questionMatrix.keySet());
            allUserIds.addAll(postMatrix.keySet());

            log.info("开始为 {} 个用户计算推荐", allUserIds.size());

            for (Long userId : allUserIds) {
                try {
                    computeAndSaveForUser(userId, questionMatrix, postMatrix);
                } catch (Exception e) {
                    log.error("用户 {} 推荐计算失败", userId, e);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("===== 全量推荐计算完成，耗时 {} ms =====", elapsed);
        } catch (Exception e) {
            log.error("全量推荐计算异常", e);
        }
    }

    @Override
    public void refreshUserRecommendations(Long userId) {
        log.info("刷新用户 {} 的推荐结果", userId);
        try {
            Map<Long, Map<Long, Double>> questionMatrix = cfEngine.buildQuestionRatingMatrix();
            Map<Long, Map<Long, Double>> postMatrix = cfEngine.buildPostRatingMatrix();
            computeAndSaveForUser(userId, questionMatrix, postMatrix);
        } catch (Exception e) {
            log.error("刷新用户 {} 推荐失败", userId, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 为单个用户计算并保存推荐结果
     */
    private void computeAndSaveForUser(Long userId,
                                       Map<Long, Map<Long, Double>> questionMatrix,
                                       Map<Long, Map<Long, Double>> postMatrix) {

        // 1. 计算题目推荐
        List<Map.Entry<Long, Double>> questionRecs;
        Map<Long, Double> userQuestionRatings = questionMatrix.get(userId);

        if (userQuestionRatings != null && userQuestionRatings.size() >= 3) {
            // 协同过滤推荐
            questionRecs = new ArrayList<>(cfEngine.generateRecommendations(userId, questionMatrix, TOP_K, TOP_N, MIN_COMMON_ITEMS_Q));
            // 如果协同过滤结果不足，用标签推荐补充
            if (questionRecs.size() < TOP_N) {
                List<Map.Entry<Long, Double>> tagRecs = cfEngine.getTagBasedRecommendations(
                        userId, questionMatrix, TOP_N - questionRecs.size(), true);
                Set<Long> existIds = questionRecs.stream()
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                for (Map.Entry<Long, Double> tagRec : tagRecs) {
                    if (!existIds.contains(tagRec.getKey())) {
                        questionRecs.add(tagRec);
                    }
                }
            }
        } else {
            // 冷启动：混合热门 + 标签推荐
            questionRecs = new ArrayList<>(cfEngine.getHotQuestionRecommendations(TOP_N));
        }

        // 2. 计算帖子推荐
        List<Map.Entry<Long, Double>> postRecs;
        Map<Long, Double> userPostRatings = postMatrix.get(userId);

        if (userPostRatings != null && userPostRatings.size() >= 2) {
            postRecs = new ArrayList<>(cfEngine.generateRecommendations(userId, postMatrix, TOP_K, TOP_N, MIN_COMMON_ITEMS_P));
            if (postRecs.size() < TOP_N) {
                List<Map.Entry<Long, Double>> tagRecs = cfEngine.getTagBasedRecommendations(
                        userId, postMatrix, TOP_N - postRecs.size(), false);
                Set<Long> existIds = postRecs.stream()
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                for (Map.Entry<Long, Double> tagRec : tagRecs) {
                    if (!existIds.contains(tagRec.getKey())) {
                        postRecs.add(tagRec);
                    }
                }
            }
        } else {
            postRecs = new ArrayList<>(cfEngine.getHotPostRecommendations(TOP_N));
        }

        // 3. 清除旧推荐并保存新推荐
        remove(new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, userId));

        List<UserRecommendation> toSave = new ArrayList<>();

        // 保存题目推荐
        for (Map.Entry<Long, Double> entry : questionRecs) {
            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(userId);
            rec.setRecommendType(TYPE_QUESTION);
            rec.setItemId(entry.getKey());
            rec.setScore(entry.getValue());
            rec.setReason(generateReason(userId, entry.getKey(), questionMatrix, true));
            toSave.add(rec);
        }

        // 保存帖子推荐
        for (Map.Entry<Long, Double> entry : postRecs) {
            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(userId);
            rec.setRecommendType(TYPE_POST);
            rec.setItemId(entry.getKey());
            rec.setScore(entry.getValue());
            rec.setReason(generateReason(userId, entry.getKey(), postMatrix, false));
            toSave.add(rec);
        }

        if (!toSave.isEmpty()) {
            saveBatch(toSave);
            log.info("用户 {} 保存了 {} 条题目推荐和 {} 条帖子推荐",
                    userId, questionRecs.size(), postRecs.size());
        }
    }

    /**
     * 生成推荐理由
     */
    private String generateReason(Long userId, Long itemId,
                                  Map<Long, Map<Long, Double>> matrix, boolean isQuestion) {
        Map<Long, Double> userRatings = matrix.get(userId);

        // 冷启动用户
        if (userRatings == null || userRatings.size() < 3) {
            return isQuestion ? "热门推荐题目" : "热门推荐帖子";
        }

        // 计算有多少相似用户交互了该物品
        int similarUserCount = 0;
        for (Map.Entry<Long, Map<Long, Double>> entry : matrix.entrySet()) {
            if (entry.getKey().equals(userId)) continue;
            if (entry.getValue().containsKey(itemId)) {
                similarUserCount++;
            }
            if (similarUserCount >= 5) break; // 避免过度计算
        }

        if (similarUserCount > 0) {
            return isQuestion ?
                    "和你兴趣相似的 " + similarUserCount + " 位用户也做了这道题" :
                    "和你兴趣相似的 " + similarUserCount + " 位用户也关注了这篇帖子";
        }

        return isQuestion ? "基于你的做题偏好推荐" : "基于你的兴趣推荐";
    }

    /**
     * 填充题目详细信息到推荐VO
     */
    private List<RecommendationVO> fillQuestionDetails(List<UserRecommendation> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取题目信息
        Set<Long> questionIds = records.stream()
                .map(UserRecommendation::getItemId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = new HashMap<>();
        try {
            List<Question> questions = questionService.listByIds(questionIds);
            if (questions != null) {
                questionMap = questions.stream()
                        .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
            }
        } catch (Exception e) {
            log.error("获取题目详情失败", e);
        }

        List<RecommendationVO> voList = new ArrayList<>();
        for (UserRecommendation rec : records) {
            RecommendationVO vo = new RecommendationVO();
            vo.setItemId(rec.getItemId());
            vo.setScore(rec.getScore());
            vo.setReason(rec.getReason());
            vo.setRecommendType(TYPE_QUESTION);

            Question question = questionMap.get(rec.getItemId());
            if (question != null) {
                vo.setQuestionTitle(question.getTitle());
                vo.setSubmitNum(question.getSubmitNum());
                vo.setAcceptedNum(question.getAcceptedNum());
                vo.setFavourNum(question.getFavourNum());
                vo.setThumbNum(question.getThumbNum());
                if (question.getTags() != null) {
                    try {
                        vo.setQuestionTags(JSONUtil.toList(question.getTags(), String.class));
                    } catch (Exception ignored) {
                        vo.setQuestionTags(Collections.emptyList());
                    }
                }
                vo.setCreateTime(question.getCreateTime());
            }
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 填充帖子详细信息到推荐VO
     */
    private List<RecommendationVO> fillPostDetails(List<UserRecommendation> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> postIds = records.stream()
                .map(UserRecommendation::getItemId)
                .collect(Collectors.toSet());

        Map<Long, Post> postMap = new HashMap<>();
        try {
            List<Post> posts = postFeignClient.listPostsByIds(postIds);
            if (posts != null) {
                postMap = posts.stream()
                        .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));
            }
        } catch (Exception e) {
            log.error("获取帖子详情失败", e);
        }

        List<RecommendationVO> voList = new ArrayList<>();
        for (UserRecommendation rec : records) {
            RecommendationVO vo = new RecommendationVO();
            vo.setItemId(rec.getItemId());
            vo.setScore(rec.getScore());
            vo.setReason(rec.getReason());
            vo.setRecommendType(TYPE_POST);

            Post post = postMap.get(rec.getItemId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
                vo.setPostCover(post.getCover());
                vo.setPostThumbNum(post.getThumbNum());
                vo.setPostFavourNum(post.getFavourNum());
                vo.setPostViewNum(post.getViewNum());
                if (post.getTags() != null) {
                    try {
                        vo.setPostTags(JSONUtil.toList(post.getTags(), String.class));
                    } catch (Exception ignored) {
                        vo.setPostTags(Collections.emptyList());
                    }
                }
                vo.setCreateTime(post.getCreateTime());
            }
            voList.add(vo);
        }
        return voList;
    }
}
