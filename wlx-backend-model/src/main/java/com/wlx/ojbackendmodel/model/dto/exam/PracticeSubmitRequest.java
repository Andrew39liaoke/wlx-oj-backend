package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PracticeSubmitRequest implements Serializable {
    private Long recordId;
    private List<AnswerItem> answers;
    private static final long serialVersionUID = 1L;
}
