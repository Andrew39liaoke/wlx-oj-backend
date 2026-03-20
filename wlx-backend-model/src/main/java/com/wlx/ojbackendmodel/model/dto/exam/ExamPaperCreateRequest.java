package com.wlx.ojbackendmodel.model.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    private static final long serialVersionUID = 1L;
}
