package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;

@Data
public class AnswerItem implements Serializable {
    private Long questionId;
    private String userAnswer;
    private static final long serialVersionUID = 1L;
}
