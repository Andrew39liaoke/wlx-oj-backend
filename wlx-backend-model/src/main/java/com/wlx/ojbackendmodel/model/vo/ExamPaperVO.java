package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ExamPaperVO implements Serializable {
    private Long id;
    private String title;
    private String description;
    private Long classId;
    private Integer totalScore;
    private Integer questionCount;
    private Integer timeLimit;
    private Integer singleCount;
    private Integer multiCount;
    private Integer status;
    private String statusLabel;
    private Date startTime;
    private Date endTime;
    private Long creatorId;
    private String creatorName;
    private Date createTime;
    private List<String> knowledgeCoverage;
    /** 当前用户是否已答 */
    private Boolean hasAnswered;
    private static final long serialVersionUID = 1L;
}
