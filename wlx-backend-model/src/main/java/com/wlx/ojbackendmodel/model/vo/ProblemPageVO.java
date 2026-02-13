package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProblemPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer classProblemId;

    private Integer id;

    private String title;

    private List<Integer> tagIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
