package com.wlx.ojbackendmodel.model.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostUpdateRequest implements Serializable {

	@NotNull(message = "id 不能为空")
	private Long id;

	/**
	 * 标题
	 */
	@NotBlank(message = "请填写标题")
	private String title;

	/**
	 * 内容
	 */
	@NotBlank(message = "请填写内容")
	private String content;

	/**
	 * 分区
	 */
	@NotBlank(message = "请选择分区")
	private String zone;

	/**
	 * 标签列表（json 数组）
	 */
	@NotNull(message = "请输入至少一个标签")
	private List<String> tags;

    private static final long serialVersionUID = 1L;
}
