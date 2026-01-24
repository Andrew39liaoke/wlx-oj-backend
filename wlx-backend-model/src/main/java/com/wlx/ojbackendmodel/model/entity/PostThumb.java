package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 帖子点赞
 */
@TableName(value = "post_thumb")
@Data
public class PostThumb implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}


