package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播间实体
 */
@TableName(value ="live_room")
@Data
public class LiveRoom implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 教师(主播)ID
     */
    private Long teacherId;

    /**
     * 直播标题
     */
    private String title;

    /**
     * 0-未开始 1-直播中 2-已结束
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

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
