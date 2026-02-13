package com.wlx.ojbackendmodel.model.dto.Class;

import com.wlx.ojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询班级学生请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClassStudentQueryRequest extends PageRequest implements Serializable {

    /**
     * 班级ID
     */
    private Long classId;

    private static final long serialVersionUID = 1L;
}
