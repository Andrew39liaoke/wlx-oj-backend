package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考试试卷表
 */
@TableName(value = "exam_paper")
@Data
public class ExamPaper implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Long classId;

    private Integer totalScore;

    private Integer questionCount;

    /**
     * 考试时限（分钟）
     */
    private Integer timeLimit;

    private Integer singleCount;

    private Integer multiCount;

    /**
     * 难度分布配置（JSON）
     */
    private String difficultyDist;

    private String knowledgeIds;

    /**
     * 状态：0-草稿，1-已发布，2-已结束
     */
    private Integer status;

    private Date startTime;

    private Date endTime;

    private Long creatorId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
