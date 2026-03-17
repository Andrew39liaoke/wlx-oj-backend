package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识点能力记录表
 */
@TableName(value = "knowledge_ability")
@Data
public class KnowledgeAbility implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeId;

    private Long recordId;

    private Integer totalScore;

    private Integer obtainedScore;

    private Integer correctCount;

    private Integer totalCount;

    /**
     * 掌握度（0~1）
     */
    private Double masteryRate;

    /**
     * 掌握等级：1-薄弱，2-一般，3-良好
     */
    private Integer masteryLevel;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
