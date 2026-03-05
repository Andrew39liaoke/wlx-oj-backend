package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 推荐结果 VO
 */
@Data
public class RecommendationVO implements Serializable {

    /**
     * 推荐物品id
     */
    private Long itemId;

    /**
     * 推荐得分
     */
    private Double score;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 推荐类型（1-题目，2-帖子）
     */
    private Integer recommendType;

    // ===== 题目相关字段 =====

    /**
     * 题目标题
     */
    private String questionTitle;

    /**
     * 题目标签
     */
    private List<String> questionTags;

    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

    /**
     * 题目收藏数
     */
    private Integer favourNum;

    /**
     * 题目点赞数
     */
    private Integer thumbNum;

    // ===== 帖子相关字段 =====

    /**
     * 帖子标题
     */
    private String postTitle;

    /**
     * 帖子封面
     */
    private String postCover;

    /**
     * 帖子标签
     */
    private List<String> postTags;

    /**
     * 帖子点赞数
     */
    private Integer postThumbNum;

    /**
     * 帖子收藏数
     */
    private Integer postFavourNum;

    /**
     * 帖子浏览数
     */
    private Integer postViewNum;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
