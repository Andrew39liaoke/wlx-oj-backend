package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;

@Data
public class ExamPaperQueryRequest implements Serializable {
    private Long classId;
    private Integer status;
    private int current = 1;
    private int pageSize = 10;
    private static final long serialVersionUID = 1L;
}
