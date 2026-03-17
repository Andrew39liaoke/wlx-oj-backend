package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class KnowledgeAbilityVO implements Serializable {
    private Long knowledgeId;
    private String knowledgeName;
    private Double masteryRate;
    private Integer masteryLevel;
    private String masteryLabel;
    private Integer correctCount;
    private Integer totalCount;
    private Integer obtainedScore;
    private Integer totalScore;
    private static final long serialVersionUID = 1L;
}
