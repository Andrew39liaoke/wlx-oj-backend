package com.wlx.ojbackendmodel.model.dto.postComment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PostCommentRequest implements Serializable {

    private Long id;

    private Long parentId;

    @NotNull(message = "帖子id不能为空")
    private Long postId;

    @NotBlank(message = "请输入内容")
    private String content;

    private Long authorId;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
