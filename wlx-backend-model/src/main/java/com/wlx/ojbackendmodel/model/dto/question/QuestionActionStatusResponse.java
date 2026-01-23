package com.wlx.ojbackendmodel.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户操作状态查询响应
 */
@Data
public class QuestionActionStatusResponse implements Serializable {

    /**
     * 用户已点赞的题目ID列表
     */
    private List<Long> thumbedQuestions;

    /**
     * 用户已收藏的题目ID列表
     */
    private List<Long> favouredQuestions;

    private static final long serialVersionUID = 1L;
}
