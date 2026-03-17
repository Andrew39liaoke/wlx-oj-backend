package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ExamQuestionUpdateRequest implements Serializable {
    private Long id;
    private String title;
    private Integer questionType;
    private List<OptionItem> options;
    private String correctAnswer;
    private Integer score;
    private Integer difficulty;
    private String knowledgeIds;
    private List<String> tags;
    private String analysis;
    private static final long serialVersionUID = 1L;
}
