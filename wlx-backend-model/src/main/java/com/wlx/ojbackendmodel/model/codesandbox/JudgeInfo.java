package com.wlx.ojbackendmodel.model.codesandbox;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内存
     */
    private Long memory;

    /**
     * 消耗时间（KB）
     */
    private Long time;

    /**
     * 通过测试用例数目
     */
    private Integer passCaseCount;

    /**
     * 总测试用例数目
     */
    private Integer totalCaseCount;

    /**
     * 通过率
     */
    private Double passRate;

    /**
     * 得分
     */
    private Double score;
}
