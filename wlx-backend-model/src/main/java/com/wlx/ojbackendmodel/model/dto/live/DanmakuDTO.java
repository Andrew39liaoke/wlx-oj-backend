package com.wlx.ojbackendmodel.model.dto.live;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 弹幕DTO
 */
@Data
public class DanmakuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotBlank(message = "弹幕内容不能为空")
    @Size(min = 1, max = 50, message ="弹幕内容必须在1到50个字符之间")
    private String content;

    private String color;

    @Min(value = 10, message = "弹幕大小必须大于10")
    @Max(value = 60, message = "弹幕大小必须小于等于60")
    private Integer size;
}
