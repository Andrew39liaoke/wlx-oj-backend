package com.wlx.ojbackendmodel.model.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostAddRequest implements Serializable {

    /**
     * 标题
     */
    @NotBlank(message = "请填写标题")
    private String title;

    /**
     * 分区
     */
    @NotBlank(message = "请选择分区")
    private String zone;

    /**
     * 内容
     */
    @NotBlank(message = "请填写内容")
    private String content;

    /**
     * 标签列表（json 数组）
     */
    @NotNull(message = "请输入至少一个标签")
    private List<String> tags;

    /**
     * 帖子封面
     */
    private String cover;

    private static final long serialVersionUID = 1L;
}
