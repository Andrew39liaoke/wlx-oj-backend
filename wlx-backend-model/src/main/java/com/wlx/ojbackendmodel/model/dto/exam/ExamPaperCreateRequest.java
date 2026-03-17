package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ExamPaperCreateRequest implements Serializable {
    private Long classId;
    private String title;
    private String description;
    private List<Long> questionIds;
    private Integer timeLimit;
    private Date startTime;
    private Date endTime;
    private static final long serialVersionUID = 1L;
}
