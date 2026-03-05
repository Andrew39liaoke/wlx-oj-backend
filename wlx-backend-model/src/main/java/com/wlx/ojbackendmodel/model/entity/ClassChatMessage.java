package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 班级实时聊天记录表
 */
@TableName(value = "class_chat_message")
@Data
public class ClassChatMessage implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 聊天内容
     */
    private String content;

    /**
     * 消息类型：0-文本，1-图片
     */
    private Integer messageType;

    /**
     * 图片URL（OSS地址）
     */
    private String imageUrl;

    /**
     * 发送时间
     */
    private Date createTime;

    /**
     * 是否删除 (0-未删除, 1-已删除)
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
