package com.wlx.ojbackendpostservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.postComment.PostCommentRequest;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import com.wlx.ojbackendpostservice.mapper.PostCommentMapper;
import com.wlx.ojbackendpostservice.service.PostCommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentService {

    /**
     * 创建评论
     * - 将请求对象的属性复制到实体对象
     * - 设置评论的用户 ID 和创建时间
     * - 保存到数据库并返回新评论的 ID
     */
    @Override
    public long createComment(PostCommentRequest req, long userId) {
        PostComment postComment = new PostComment();
        BeanUtils.copyProperties(req, postComment);
        postComment.setUserId(userId);
        postComment.setCreateTime(new Date());
        boolean result = this.save(postComment);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return postComment.getId();
    }

    /**
     * 删除评论
     * - 根据 id 查询评论，找不到则抛出 NOT_FOUND_ERROR
     * - 校验当前用户是否为评论作者，若不是则抛出 NO_AUTH_ERROR
     * - 删除评论并在失败时抛出 OPERATION_ERROR
     */
    @Override
    public boolean deleteComment(long id, long userId) {
        PostComment comment = this.getById(id);
        ThrowUtils.throwIf(comment == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!comment.getUserId().equals(userId), ResopnseCodeEnum.NO_AUTH_ERROR);
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return result;
    }

    /**
     * 更新评论
     * - 校验目标评论是否存在，不存在抛出 NOT_FOUND_ERROR
     * - 校验当前用户是否为评论作者，若不是抛出 NO_AUTH_ERROR
     * - 将请求属性复制到新实体，保留原作者和创建时间后执行更新
     */
    @Override
    public boolean updateComment(PostCommentRequest req, long userId) {
        PostComment exist = this.getById(req.getId());
        ThrowUtils.throwIf(exist == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!exist.getUserId().equals(userId), ResopnseCodeEnum.NO_AUTH_ERROR);
        PostComment comment = new PostComment();
        BeanUtils.copyProperties(req, comment);
        comment.setUserId(exist.getUserId());
        comment.setCreateTime(exist.getCreateTime());
        boolean result = this.updateById(comment);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return result;
    }

    /**
     * 根据帖子ID获取评论列表
     */
    @Override
    public List<PostComment> getCommentsByPostId(long postId) {
        QueryWrapper<PostComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("postId", postId);
        queryWrapper.orderByAsc("createTime");
        return this.list(queryWrapper);
    }
}


