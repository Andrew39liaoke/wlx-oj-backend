package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RecommendedQuestionVO implements Serializable {
    private Long questionId;
    private String title;
    private Integer questionType;
    private Integer difficulty;
    private String difficultyLabel;
    private List<String> knowledgeTags;
    private Integer recommendType;
    private String recommendReason;
    private Integer priority;
    private Boolean isPracticed;
    private static final long serialVersionUID = 1L;
}
