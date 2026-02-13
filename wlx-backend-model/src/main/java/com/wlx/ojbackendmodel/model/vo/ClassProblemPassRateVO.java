package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClassProblemPassRateVO implements Serializable {

    private String problemTitle;

    private Integer difficulty;

    private Integer attemptedStudents;

    private Double passRate;

    private Integer totalSubmissions;
}