package com.wlx.ojbackendmodel.model.dto.ai;

import com.wlx.ojbackendmodel.model.enums.AiTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Ai输入DTO
 *
 * @author 竹林听雨
 * @date 2024/12/27
 */
@Data
public class AiInputDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6099069246684387027L;

    @NotNull(message = "用户id不能为空")
    private Long userId;

    @NotNull(message = "聊天id不能为空")
    private String chatId;

    @NotNull(message = "对话内容不能为空")
    private String prompt;

    @NotNull(message = "对话类型不能为空")
    private AiTypeEnum type;

    private Long problemId;

    private Long classId;
}
