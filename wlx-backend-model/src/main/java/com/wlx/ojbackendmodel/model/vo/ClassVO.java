package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ClassVO implements Serializable {

    private static final long serialVersionUID = -8175862502928320993L;

    private Long id;

    private Long teacherId;

    private String teacherName;

    private String name;

    private String invitationCode;

    private Integer joinNumber;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}