package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;

@Data
public class KnowledgePointRequest implements Serializable {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Long classId;
    private Integer sortOrder;
    private static final long serialVersionUID = 1L;
}
