package com.wlx.ojbackendmodel.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户操作状态查询请求
 */
@Data
public class QuestionActionStatusRequest implements Serializable {

    /**
     * 题目ID列表
     */
    private List<Long> questionIds;

    /**
     * 用户ID
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
