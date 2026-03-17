package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ExamAnswerSubmitRequest implements Serializable {
    private Long paperId;
    private Long recordId;
    private List<AnswerItem> answers;
    private static final long serialVersionUID = 1L;
}
