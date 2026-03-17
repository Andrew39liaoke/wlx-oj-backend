package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PracticeResultVO implements Serializable {
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracyRate;
    private List<ExamAnswerDetailVO> details;
    private MasteryUpdate updatedMastery;
    private static final long serialVersionUID = 1L;

    @Data
    public static class MasteryUpdate implements Serializable {
        private Long knowledgeId;
        private String knowledgeName;
        private Double oldMasteryRate;
        private Double newMasteryRate;
        private Double improvement;
        private static final long serialVersionUID = 1L;
    }
}
