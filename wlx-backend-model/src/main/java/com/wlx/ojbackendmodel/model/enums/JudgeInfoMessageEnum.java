package com.wlx.ojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题信息消息枚举
 *
 */
public enum JudgeInfoMessageEnum {

    ACCEPTED("执行通过", "执行通过"),
    WRONG_ANSWER("错误解答", "错误解答"),
    COMPILE_ERROR("编译出错", "编译出错"),
    MEMORY_LIMIT_EXCEEDED("超出内存限制", "超出内存限制"),
    TIME_LIMIT_EXCEEDED("超出时间限制", "超出时间限制"),
    PRESENTATION_ERROR("展示错误", "展示错误"),
    WAITING("等待中", "等待中"),
    OUTPUT_LIMIT_EXCEEDED("超出输出限制", "超出输出限制"),
    DANGEROUS_OPERATION("危险操作", "危险操作"),
    RUNTIME_ERROR("执行出错", "执行出错"),
    SYSTEM_ERROR("内部出错", "内部出错");

    private final String text;

    private final String value;

    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
