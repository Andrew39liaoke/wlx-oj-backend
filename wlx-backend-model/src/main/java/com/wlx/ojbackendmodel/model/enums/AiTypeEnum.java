package com.wlx.ojbackendmodel.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiTypeEnum {
    /**
     * 普通聊天
     */
    CHAT(0, "chat"),
    /**
     * 代码助手
     */
    CODE(1, "code");

    private final int value;
    @EnumValue
    private final String desc;
}