package com.wlx.ojbackendmodel.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
public class ClassProblemSubmissionsVO implements Serializable {

    private String studentName;

    private String totalSubmissions;

    private String totalPasses;

    private Double passRate;
}
