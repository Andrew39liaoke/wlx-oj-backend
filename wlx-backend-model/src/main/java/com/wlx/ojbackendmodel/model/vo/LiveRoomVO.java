package com.wlx.ojbackendmodel.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播间 VO
 */
@Data
public class LiveRoomVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long classId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teacherId;

    /**
     * 教师昵称
     */
    private String teacherName;

    /**
     * 教师头像
     */
    private String teacherAvatar;

    /**
     * 直播标题
     */
    private String title;

    /**
     * 直播状态：0-未开始 1-直播中 2-已结束
     */
    private Integer status;

    /**
     * SRS 流ID
     */
    private String streamId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 观看人数
     */
    private Integer viewerCount;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
