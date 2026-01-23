package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统角色表
 *
 */
@TableName(value = "role")
@Data
public class Role implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 值
     */
    private String value;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
