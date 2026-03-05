package com.wlx.ojbackendquestionservice.service;

import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendmodel.model.entity.*;
import com.wlx.ojbackendserviceclient.service.PostFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤引擎
 * 基于用户的协同过滤算法（User-Based CF）
 * 题目数据通过本地 Service 直接查询，帖子数据通过 PostFeignClient 内部调用
 */
@Component
@Slf4j
public class CollaborativeFilteringEngine {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionThumbService questionThumbService;

    @Resource
    private QuestionFavourService questionFavourService;

    @Resource
    private PostFeignClient postFeignClient;

    // ===================== 题目推荐 =====================

    /**
     * 构建用户-题目评分矩阵
     * 数据来源：question_submit + question_thumb + question_favour
     *
     * @return Map<userId, Map<questionId, score>>
     */
    public Map<Long, Map<Long, Double>> buildQuestionRatingMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        // 1. 查询所有提交记录（本地直接查询）
        List<QuestionSubmit> submits = questionSubmitService.list();
        if (submits != null) {
            for (QuestionSubmit submit : submits) {
                Long userId = submit.getUserId();
                Long questionId = submit.getQuestionId();
                matrix.computeIfAbsent(userId, k -> new HashMap<>());
                Map<Long, Double> userRatings = matrix.get(userId);
                // 提交且通过 = 3.0，提交未通过 = 1.0
                double submitScore = (submit.getStatus() != null && submit.getStatus() == 2) ? 3.0 : 1.0;
                // 取最高分（可能有多次提交）
                userRatings.merge(questionId, submitScore, Math::max);
            }
        }

        // 2. 查询所有题目点赞记录（本地直接查询）
        List<QuestionThumb> thumbs = questionThumbService.list();
        if (thumbs != null) {
            for (QuestionThumb thumb : thumbs) {
                Long userId = thumb.getUserId();
                Long questionId = thumb.getQuestionId();
                matrix.computeIfAbsent(userId, k -> new HashMap<>());
                // 点赞 +1.0
                matrix.get(userId).merge(questionId, 1.0, Double::sum);
            }
        }

        // 3. 查询所有题目收藏记录（本地直接查询）
        List<QuestionFavour> favours = questionFavourService.list();
        if (favours != null) {
            for (QuestionFavour favour : favours) {
                Long userId = favour.getUserId();
                Long questionId = favour.getQuestionId();
                matrix.computeIfAbsent(userId, k -> new HashMap<>());
                // 收藏 +1.5
                matrix.get(userId).merge(questionId, 1.5, Double::sum);
            }
        }

        // 4. 限制最大评分为5.0
        for (Map<Long, Double> userRatings : matrix.values()) {
            userRatings.replaceAll((k, v) -> Math.min(5.0, v));
        }

        log.info("题目评分矩阵构建完成，用户数：{}，总评分项：{}",
                matrix.size(),
                matrix.values().stream().mapToInt(Map::size).sum());

        return matrix;
    }

    // ===================== 帖子推荐 =====================

    /**
     * 构建用户-帖子评分矩阵
     * 数据来源：post_thumb + post_favour + post_comment（通过 PostFeignClient 内部调用）
     *
     * @return Map<userId, Map<postId, score>>
     */
    public Map<Long, Map<Long, Double>> buildPostRatingMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        // 1. 查询所有帖子点赞记录（Feign 内部调用）
        List<PostThumb> thumbs = postFeignClient.listAllPostThumbs();
        if (thumbs != null) {
            for (PostThumb thumb : thumbs) {
                Long userId = thumb.getUserId();
                Long postId = thumb.getPostId();
                matrix.computeIfAbsent(userId, k -> new HashMap<>());
                // 点赞 +1.5
                matrix.get(userId).merge(postId, 1.5, Double::sum);
            }
        }

        // 2. 查询所有帖子收藏记录（Feign 内部调用）
        List<PostFavour> favours = postFeignClient.listAllPostFavours();
        if (favours != null) {
            for (PostFavour favour : favours) {
                Long userId = favour.getUserId();
                Long postId = favour.getPostId();
                matrix.computeIfAbsent(userId, k -> new HashMap<>());
                // 收藏 +2.0
                matrix.get(userId).merge(postId, 2.0, Double::sum);
            }
        }

        // 3. 查询所有帖子评论记录（Feign 内部调用）
        List<PostComment> comments = postFeignClient.listAllPostComments();
        if (comments != null) {
            // 每个用户对每个帖子只计一次评论分
            Set<String> userPostSet = new HashSet<>();
            for (PostComment comment : comments) {
                Long userId = comment.getUserId();
                Long postId = comment.getPostId();
                String key = userId + "_" + postId;
                if (userPostSet.add(key)) {
                    matrix.computeIfAbsent(userId, k -> new HashMap<>());
                    // 评论 +1.5
                    matrix.get(userId).merge(postId, 1.5, Double::sum);
                }
            }
        }

        // 4. 限制最大评分为5.0
        for (Map<Long, Double> userRatings : matrix.values()) {
            userRatings.replaceAll((k, v) -> Math.min(5.0, v));
        }

        log.info("帖子评分矩阵构建完成，用户数：{}，总评分项：{}",
                matrix.size(),
                matrix.values().stream().mapToInt(Map::size).sum());

        return matrix;
    }

    // ===================== 核心协同过滤算法 =====================

    /**
     * 计算两个用户之间的余弦相似度
     */
    public double cosineSimilarity(Map<Long, Double> userA, Map<Long, Double> userB, int minCommonItems) {
        Set<Long> commonItems = new HashSet<>(userA.keySet());
        commonItems.retainAll(userB.keySet());

        if (commonItems.size() < minCommonItems) {
            return 0.0;
        }

        double dotProduct = 0, normA = 0, normB = 0;
        for (Long item : commonItems) {
            double a = userA.get(item);
            double b = userB.get(item);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 为指定用户生成推荐列表
     */
    public List<Map.Entry<Long, Double>> generateRecommendations(
            Long targetUserId,
            Map<Long, Map<Long, Double>> ratingMatrix,
            int topK, int topN, int minCommonItems) {

        Map<Long, Double> targetRatings = ratingMatrix.get(targetUserId);

        if (targetRatings == null || targetRatings.size() < 3) {
            return Collections.emptyList();
        }

        // 1. 计算与所有其他用户的相似度
        List<Map.Entry<Long, Double>> similarities = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : ratingMatrix.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) continue;

            Map<Long, Double> otherRatings = entry.getValue();
            if (otherRatings.size() < 3) continue;

            double sim = cosineSimilarity(targetRatings, otherRatings, minCommonItems);
            if (sim > 0) {
                similarities.add(new AbstractMap.SimpleEntry<>(otherUserId, sim));
            }
        }

        // 2. 取 Top-K 相似用户
        similarities.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Map.Entry<Long, Double>> topKUsers = similarities.stream()
                .limit(topK)
                .collect(Collectors.toList());

        if (topKUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 聚合推荐评分
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> simSums = new HashMap<>();

        for (Map.Entry<Long, Double> simEntry : topKUsers) {
            Long neighborId = simEntry.getKey();
            double sim = simEntry.getValue();
            Map<Long, Double> neighborRatings = ratingMatrix.get(neighborId);

            for (Map.Entry<Long, Double> ratingEntry : neighborRatings.entrySet()) {
                Long itemId = ratingEntry.getKey();
                double rating = ratingEntry.getValue();

                if (targetRatings.containsKey(itemId)) continue;

                candidateScores.merge(itemId, sim * rating, Double::sum);
                simSums.merge(itemId, Math.abs(sim), Double::sum);
            }
        }

        // 4. 计算预测评分并排序
        List<Map.Entry<Long, Double>> predictions = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long itemId = entry.getKey();
            double weightedSum = entry.getValue();
            double simSum = simSums.get(itemId);
            double predictedScore = simSum > 0 ? weightedSum / simSum : 0;
            predictions.add(new AbstractMap.SimpleEntry<>(itemId, predictedScore));
        }

        predictions.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return predictions.stream().limit(topN).collect(Collectors.toList());
    }

    /**
     * 获取热门题目推荐（冷启动场景，本地直接查询）
     */
    public List<Map.Entry<Long, Double>> getHotQuestionRecommendations(int topN) {
        List<Question> questions = questionService.list();
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }

        return questions.stream()
                .sorted((a, b) -> {
                    int scoreA = safeInt(a.getFavourNum()) + safeInt(a.getThumbNum()) + safeInt(a.getAcceptedNum());
                    int scoreB = safeInt(b.getFavourNum()) + safeInt(b.getThumbNum()) + safeInt(b.getAcceptedNum());
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(topN)
                .map(q -> (Map.Entry<Long, Double>) new AbstractMap.SimpleEntry<>(q.getId(),
                        (double) (safeInt(q.getFavourNum()) + safeInt(q.getThumbNum()) + safeInt(q.getAcceptedNum()))))
                .collect(Collectors.toList());
    }

    /**
     * 获取热门帖子推荐（冷启动场景，Feign 内部调用）
     */
    public List<Map.Entry<Long, Double>> getHotPostRecommendations(int topN) {
        List<Post> posts = postFeignClient.listAllPosts();
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }

        return posts.stream()
                .sorted((a, b) -> {
                    int scoreA = safeInt(a.getFavourNum()) + safeInt(a.getThumbNum()) + safeInt(a.getViewNum());
                    int scoreB = safeInt(b.getFavourNum()) + safeInt(b.getThumbNum()) + safeInt(b.getViewNum());
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(topN)
                .map(p -> (Map.Entry<Long, Double>) new AbstractMap.SimpleEntry<>(p.getId(),
                        (double) (safeInt(p.getFavourNum()) + safeInt(p.getThumbNum()) + safeInt(p.getViewNum()))))
                .collect(Collectors.toList());
    }

    /**
     * 获取基于标签的推荐补充（用户行为较少时）
     */
    public List<Map.Entry<Long, Double>> getTagBasedRecommendations(
            Long targetUserId, Map<Long, Map<Long, Double>> ratingMatrix,
            int topN, boolean isQuestion) {

        Map<Long, Double> targetRatings = ratingMatrix.get(targetUserId);
        if (targetRatings == null || targetRatings.isEmpty()) {
            return isQuestion ? getHotQuestionRecommendations(topN) : getHotPostRecommendations(topN);
        }

        Map<String, Integer> tagFrequency = new HashMap<>();
        Set<Long> interactedIds = targetRatings.keySet();

        if (isQuestion) {
            // 本地直接查询题目
            List<Question> questions = questionService.listByIds(interactedIds);
            if (questions != null) {
                for (Question q : questions) {
                    if (q.getTags() != null) {
                        List<String> tags = JSONUtil.toList(q.getTags(), String.class);
                        for (String tag : tags) {
                            tagFrequency.merge(tag, 1, Integer::sum);
                        }
                    }
                }
            }

            // 本地直接查询所有题目
            List<Question> allQuestions = questionService.list();
            if (allQuestions == null) return Collections.emptyList();

            return allQuestions.stream()
                    .filter(q -> !interactedIds.contains(q.getId()))
                    .map(q -> {
                        double score = 0;
                        if (q.getTags() != null) {
                            List<String> tags = JSONUtil.toList(q.getTags(), String.class);
                            for (String tag : tags) {
                                score += tagFrequency.getOrDefault(tag, 0);
                            }
                        }
                        return new AbstractMap.SimpleEntry<>(q.getId(), score);
                    })
                    .filter(e -> e.getValue() > 0)
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(topN)
                    .map(e -> (Map.Entry<Long, Double>) e)
                    .collect(Collectors.toList());
        } else {
            // Feign 内部调用查询帖子
            List<Post> posts = postFeignClient.listPostsByIds(interactedIds);
            if (posts != null) {
                for (Post p : posts) {
                    if (p.getTags() != null) {
                        try {
                            List<String> tags = JSONUtil.toList(p.getTags(), String.class);
                            for (String tag : tags) {
                                tagFrequency.merge(tag, 1, Integer::sum);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            List<Post> allPosts = postFeignClient.listAllPosts();
            if (allPosts == null) return Collections.emptyList();

            return allPosts.stream()
                    .filter(p -> !interactedIds.contains(p.getId()))
                    .map(p -> {
                        double score = 0;
                        if (p.getTags() != null) {
                            try {
                                List<String> tags = JSONUtil.toList(p.getTags(), String.class);
                                for (String tag : tags) {
                                    score += tagFrequency.getOrDefault(tag, 0);
                                }
                            } catch (Exception ignored) {}
                        }
                        return new AbstractMap.SimpleEntry<>(p.getId(), score);
                    })
                    .filter(e -> e.getValue() > 0)
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(topN)
                    .map(e -> (Map.Entry<Long, Double>) e)
                    .collect(Collectors.toList());
        }
    }

    private int safeInt(Integer val) {
        return val != null ? val : 0;
    }
}
