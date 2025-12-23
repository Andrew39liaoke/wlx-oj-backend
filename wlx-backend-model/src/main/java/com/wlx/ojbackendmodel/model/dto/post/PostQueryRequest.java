package com.wlx.ojbackendmodel.model.dto.post;

import com.wlx.ojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户Id
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 分区
     */
    private String zone;

    /**
     * 标签列表（json 数组）
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
