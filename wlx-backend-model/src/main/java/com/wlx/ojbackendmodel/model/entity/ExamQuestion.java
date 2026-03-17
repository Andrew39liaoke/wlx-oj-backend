package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考试题目表（选择题）
 */
@TableName(value = "exam_question")
@Data
public class ExamQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目内容
     */
    private String title;

    /**
     * 题型：1-单选题，2-多选题
     */
    private Integer questionType;

    /**
     * 选项列表（JSON数组）
     */
    private String options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 题目分值
     */
    private Integer score;

    /**
     * 难度等级：1-简单，2-中等，3-困难
     */
    private Integer difficulty;

    /**
     * 关联知识点ID列表，逗号分隔
     */
    private String knowledgeIds;

    /**
     * 标签列表（JSON数组）
     */
    private String tags;

    /**
     * 题目解析
     */
    private String analysis;

    /**
     * 创建者用户ID
     */
    private Long userId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
