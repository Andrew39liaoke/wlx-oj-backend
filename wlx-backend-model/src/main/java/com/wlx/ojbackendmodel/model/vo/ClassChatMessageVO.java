package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Data
public class ClassChatMessageVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long classId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;
    private String userName;
    private String userAvatar;
    private String content;
    private Integer messageType;
    private String imageUrl;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
