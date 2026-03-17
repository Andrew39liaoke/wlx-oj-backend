package com.wlx.ojbackendmodel.model.vo;

import com.wlx.ojbackendmodel.model.dto.exam.OptionItem;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 答题用试卷VO（不含答案）
 */
@Data
public class ExamTakeVO implements Serializable {
    private Long paperId;
    private String title;
    private Integer timeLimit;
    private Integer totalScore;
    private Integer questionCount;
    private List<TakeQuestionItem> questions;
    private static final long serialVersionUID = 1L;

    @Data
    public static class TakeQuestionItem implements Serializable {
        private Long id;
        private Integer questionOrder;
        private Integer questionType;
        private String title;
        private List<OptionItem> options;
        private Integer score;
        private static final long serialVersionUID = 1L;
    }
}
