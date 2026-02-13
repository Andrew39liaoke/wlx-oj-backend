package com.wlx.ojbackendmodel.model.dto.ai;

import lombok.Data;

import java.io.Serializable;
@Data

public class AIToolsDTO implements Serializable {

    private Integer userId;

    private String userName;

    private Integer classId;

    private String className;

    private Integer tagId;

    private String tagName;

    private Integer problemId;

    private String problemTitle;

    private Integer problemGrade;
}
