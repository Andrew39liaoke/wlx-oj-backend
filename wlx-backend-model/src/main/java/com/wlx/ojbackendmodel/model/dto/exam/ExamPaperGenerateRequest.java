package com.wlx.ojbackendmodel.model.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ExamPaperGenerateRequest implements Serializable {
    private Long classId;
    private String title;
    private String description;
    private Integer totalQuestions = 20;
    private Integer singleCount = 14;
    private Integer multiCount = 6;
    private Integer easyCount = 6;
    private Integer mediumCount = 10;
    private Integer hardCount = 4;
    private List<Long> knowledgeIds;
    private Integer timeLimit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
    private static final long serialVersionUID = 1L;
}
