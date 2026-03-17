package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 练习推荐记录表
 */
@TableName(value = "practice_recommendation")
@Data
public class PracticeRecommendation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long recordId;

    private Long knowledgeId;

    private Long questionId;

    /**
     * 推荐类型：1-规则，2-知识图谱，3-相似度
     */
    private Integer recommendType;

    private String recommendReason;

    private Integer priority;

    private Integer difficulty;

    /**
     * 是否已练习：0-未练习，1-已练习
     */
    private Integer isPracticed;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
