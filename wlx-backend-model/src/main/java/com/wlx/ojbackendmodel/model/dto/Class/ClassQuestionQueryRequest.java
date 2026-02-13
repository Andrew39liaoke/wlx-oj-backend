package com.wlx.ojbackendmodel.model.dto.Class;

import com.wlx.ojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClassQuestionQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户Id
     */
    private Long classId;

    private static final long serialVersionUID = 1L;
}
