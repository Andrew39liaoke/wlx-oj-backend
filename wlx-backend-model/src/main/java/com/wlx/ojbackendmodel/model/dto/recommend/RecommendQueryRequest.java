package com.wlx.ojbackendmodel.model.dto.recommend;

import com.wlx.ojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 推荐查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RecommendQueryRequest extends PageRequest implements Serializable {

    /**
     * 推荐类型（1-题目，2-帖子）
     */
    private Integer recommendType;

    private static final long serialVersionUID = 1L;
}
