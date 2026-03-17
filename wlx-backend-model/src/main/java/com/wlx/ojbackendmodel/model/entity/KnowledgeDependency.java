package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识点依赖关系表（知识图谱）
 */
@TableName(value = "knowledge_dependency")
@Data
public class KnowledgeDependency implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromKnowledgeId;

    private Long toKnowledgeId;

    /**
     * 依赖类型：1-前置依赖，2-关联关系
     */
    private Integer dependencyType;

    private Double weight;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
