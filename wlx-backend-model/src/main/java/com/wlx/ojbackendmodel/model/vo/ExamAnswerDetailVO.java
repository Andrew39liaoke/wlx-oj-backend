package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class ExamAnswerDetailVO implements Serializable {
    private Long questionId;
    private Integer questionOrder;
    private String questionTitle;
    private Integer questionType;
    private String userAnswer;
    private String correctAnswer;
    private Integer isCorrect;
    private Integer scoreObtained;
    private Integer scoreFull;
    private String analysis;
    private static final long serialVersionUID = 1L;
}
