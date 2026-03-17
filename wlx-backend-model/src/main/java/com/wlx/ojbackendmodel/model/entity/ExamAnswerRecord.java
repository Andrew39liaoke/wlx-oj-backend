package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户答题记录表
 */
@TableName(value = "exam_answer_record")
@Data
public class ExamAnswerRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long userId;

    private Integer totalScore;

    private Integer correctCount;

    private Integer totalCount;

    private Double accuracyRate;

    private Date startTime;

    private Date submitTime;

    /**
     * 用时（秒）
     */
    private Integer timeSpent;

    /**
     * 状态：0-答题中，1-已提交，2-已批改
     */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
