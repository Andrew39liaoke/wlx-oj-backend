package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class KnowledgeGraphVO implements Serializable {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private static final long serialVersionUID = 1L;

    @Data
    public static class GraphNode implements Serializable {
        private Long id;
        private String name;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class GraphEdge implements Serializable {
        private Long from;
        private Long to;
        private Integer type;
        private Double weight;
        private static final long serialVersionUID = 1L;
    }
}
