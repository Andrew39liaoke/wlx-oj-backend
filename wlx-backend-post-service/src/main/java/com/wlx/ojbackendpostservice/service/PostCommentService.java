package com.wlx.ojbackendpostservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.postComment.PostCommentRequest;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import java.util.List;

public interface PostCommentService extends IService<PostComment> {

    /**
     * 创建评论，返回新评论 id
     */
    long createComment(PostCommentRequest req, long userId);

    /**
     * 删除评论，仅作者可删除
     */
    boolean deleteComment(long id, long userId);

    /**
     * 更新评论，仅作者可更新
     */
    boolean updateComment(PostCommentRequest req, long userId);

    /**
     * 根据帖子ID获取评论列表
     */
    List<PostComment> getCommentsByPostId(long postId);
}


