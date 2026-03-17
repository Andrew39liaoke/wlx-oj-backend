package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class AbilityReportVO implements Serializable {
    private Long recordId;
    private Long userId;
    private String examTitle;
    private Integer totalScore;
    private Integer totalFullScore;
    private Double accuracyRate;
    private Integer timeSpent;
    private List<KnowledgeAbilityVO> knowledgeAbilities;
    private List<KnowledgeAbilityVO> weakKnowledges;
    private static final long serialVersionUID = 1L;
}
