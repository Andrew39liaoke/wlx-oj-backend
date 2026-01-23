package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目收藏
 */
@TableName(value = "question_favour")
@Data
public class QuestionFavour implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long questionId;

    private Long userId;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
