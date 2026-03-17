package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ExamResultVO implements Serializable {
    private Long recordId;
    private Integer totalScore;
    private Integer totalFullScore;
    private Integer correctCount;
    private Integer totalCount;
    private Double accuracyRate;
    private Integer timeSpent;
    private List<ExamAnswerDetailVO> details;
    private static final long serialVersionUID = 1L;
}
