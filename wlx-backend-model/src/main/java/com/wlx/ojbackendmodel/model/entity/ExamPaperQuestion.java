package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 试卷题目关联表
 */
@TableName(value = "exam_paper_question")
@Data
public class ExamPaperQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long questionId;

    private Integer questionOrder;

    private Integer score;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
