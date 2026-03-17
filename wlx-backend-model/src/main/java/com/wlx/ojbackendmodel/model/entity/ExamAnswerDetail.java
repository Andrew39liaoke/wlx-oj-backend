package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 答题详情表
 */
@TableName(value = "exam_answer_detail")
@Data
public class ExamAnswerDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long questionId;

    private String userAnswer;

    /**
     * 是否正确：0-错误，1-正确，2-部分正确
     */
    private Integer isCorrect;

    private Integer scoreObtained;

    private Integer scoreFull;

    private Integer questionOrder;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
