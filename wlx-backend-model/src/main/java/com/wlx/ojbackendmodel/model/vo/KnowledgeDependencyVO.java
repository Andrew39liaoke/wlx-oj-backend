package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识依赖关系视图对象
 */
@Data
public class KnowledgeDependencyVO implements Serializable {
    private Long id;
    private Long fromKnowledgeId;
    private String fromKnowledgeName;
    private Long toKnowledgeId;
    private String toKnowledgeName;
    private Integer dependencyType; // 1-前置依赖，2-关联关系
    private Double weight;
    private Date createTime;
    
    private static final long serialVersionUID = 1L;
}
