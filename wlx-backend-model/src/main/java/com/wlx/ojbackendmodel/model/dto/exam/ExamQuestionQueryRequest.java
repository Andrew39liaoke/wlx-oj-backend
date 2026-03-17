package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;

@Data
public class ExamQuestionQueryRequest implements Serializable {
    private Long id;
    private String title;
    private Integer questionType;
    private Integer difficulty;
    private String knowledgeIds;
    private Long paperId;
    private Long userId;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
    private static final long serialVersionUID = 1L;
}
