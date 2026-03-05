package com.wlx.ojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目提交枚举
 *
 */
public enum QuestionSubmitStatusEnum {

    // 0 - 待判题、1 - 判题中、2~12 依次对应 JudgeInfoMessageEnum
    WAITING("待判题", 0),
    RUNNING("判题中", 1),
    ACCEPTED("执行通过", 2),
    WRONG_ANSWER("错误解答", 3),
    COMPILE_ERROR("编译出错", 4),
    MEMORY_LIMIT_EXCEEDED("超出内存限制", 5),
    TIME_LIMIT_EXCEEDED("超出时间限制", 6),
    PRESENTATION_ERROR("展示错误", 7),
    OUTPUT_LIMIT_EXCEEDED("超出输出限制", 8),
    DANGEROUS_OPERATION("危险操作", 9),
    RUNTIME_ERROR("执行出错", 10),
    SYSTEM_ERROR("内部出错", 11);

    private final String text;

    private final Integer value;

    QuestionSubmitStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static QuestionSubmitStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (QuestionSubmitStatusEnum anEnum : QuestionSubmitStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据 text 获取枚举（用于从判题消息映射到提交状态）
     *
     * @param text
     * @return
     */
    public static QuestionSubmitStatusEnum getEnumByText(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return null;
        }
        for (QuestionSubmitStatusEnum anEnum : QuestionSubmitStatusEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
