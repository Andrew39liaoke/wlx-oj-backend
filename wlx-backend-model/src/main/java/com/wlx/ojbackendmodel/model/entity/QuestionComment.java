package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目评论
 */
@TableName(value = "question_comment")
@Data
public class QuestionComment implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tags;

    private Long parentId;

    private Long questionId;

    private String content;

    private Long authorId;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}


