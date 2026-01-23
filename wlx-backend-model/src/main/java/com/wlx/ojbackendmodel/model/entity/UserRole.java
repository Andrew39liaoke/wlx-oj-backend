package com.wlx.ojbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关系表
 *
 */
@TableName(value = "user_role")
@Data
public class UserRole implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色id，数据来源于role表的主键
     */
    private Long roleId;

    /**
     * 用户id，数据来源于user表的主键
     */
    private Long userId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
