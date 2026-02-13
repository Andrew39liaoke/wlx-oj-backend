package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 班级题目关联表
 * @TableName class_problem
 */
@TableName(value ="class_problem")
@Data
public class ClassProblem implements Serializable {
    /**
     * 班级题目id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库id
     */
    private Long problemId;

    /**
     * 班级id
     */
    private Long classId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
