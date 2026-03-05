package com.wlx.ojbackendmodel.model.dto.Class;

import lombok.Data;
import java.io.Serializable;

/**
 * 移除班级学生请求
 */
@Data
public class ClassStudentRemoveRequest implements Serializable {

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 操作人ID（前端可以不传或者由后端再覆盖，根据题目要求这里增加一个用户ID）
     */
    private Long userId;

    /**
     * 用户角色
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
