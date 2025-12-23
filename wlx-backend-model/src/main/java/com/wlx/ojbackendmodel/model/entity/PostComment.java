package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 帖子评论
 */
@TableName(value = "post_comment")
@Data
public class PostComment implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;

    private Long postId;

    private String content;

    private Long userId;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}


