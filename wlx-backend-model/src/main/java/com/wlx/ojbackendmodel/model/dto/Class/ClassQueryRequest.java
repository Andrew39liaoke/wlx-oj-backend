package com.wlx.ojbackendmodel.model.dto.Class;

import com.wlx.ojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClassQueryRequest extends PageRequest implements Serializable {

    /**
     * 班级名称（搜索关键词）
     */
    private String className;

    private static final long serialVersionUID = 1L;
}
