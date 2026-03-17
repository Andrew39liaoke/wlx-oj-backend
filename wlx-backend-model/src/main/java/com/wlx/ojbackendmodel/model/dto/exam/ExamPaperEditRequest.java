package com.wlx.ojbackendmodel.model.dto.exam;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 编辑试卷请求
 */
@Data
public class ExamPaperEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 考试时长（分钟）
     */
    private Integer examTime;

    private static final long serialVersionUID = 1L;
}
