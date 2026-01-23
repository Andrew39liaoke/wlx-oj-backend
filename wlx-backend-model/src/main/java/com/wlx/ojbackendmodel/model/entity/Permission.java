package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统权限表
 *
 */
@TableName(value = "permission")
@Data
public class Permission implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 接口路径
     */
    private String url;

    /**
     * 请求方式（0-get；1-post）
     */
    private Integer method;

    /**
     * 服务名
     */
    private String service;

    /**
     * 父级权限id
     */
    private Long parentId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
