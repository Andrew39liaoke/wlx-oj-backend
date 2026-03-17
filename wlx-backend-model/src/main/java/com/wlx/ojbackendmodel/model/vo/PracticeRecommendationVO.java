package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PracticeRecommendationVO implements Serializable {
    private Long knowledgeId;
    private String knowledgeName;
    private Double masteryRate;
    private String masteryLabel;
    private Integer totalRecommended;
    private Integer practicedCount;
    private List<RecommendedQuestionVO> questions;
    private static final long serialVersionUID = 1L;
}
