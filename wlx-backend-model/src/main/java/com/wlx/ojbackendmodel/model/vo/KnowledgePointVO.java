package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class KnowledgePointVO implements Serializable {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Long classId;
    private Integer sortOrder;
    private List<KnowledgePointVO> children;
    private static final long serialVersionUID = 1L;
}
