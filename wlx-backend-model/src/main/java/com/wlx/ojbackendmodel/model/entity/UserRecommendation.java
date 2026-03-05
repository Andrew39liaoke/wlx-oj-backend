package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户推荐结果
 */
@TableName(value = "user_recommendation")
@Data
public class UserRecommendation implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 推荐类型（1-题目，2-帖子）
     */
    private Integer recommendType;

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
     * 生成时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
