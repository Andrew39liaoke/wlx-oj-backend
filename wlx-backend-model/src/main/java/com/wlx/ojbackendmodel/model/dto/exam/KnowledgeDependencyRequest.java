package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class KnowledgeDependencyRequest implements Serializable {
    private List<DependencyItem> dependencies;
    private static final long serialVersionUID = 1L;

    @Data
    public static class DependencyItem implements Serializable {
        private Long fromKnowledgeId;
        private Long toKnowledgeId;
        private Integer dependencyType;
        private Double weight;
        private static final long serialVersionUID = 1L;
    }
}
