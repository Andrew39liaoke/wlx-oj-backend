package com.wlx.ojbackendpostservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.entity.PostThumb;
import com.wlx.ojbackendmodel.model.vo.PostVO;

public interface PostThumbService extends IService<PostThumb> {

    /**
     * 点赞帖子
     */
    boolean addThumb(Long postId, Long userId);

    /**
     * 取消点赞
     */
    boolean removeThumb(Long postId, Long userId);

    /**
     * 根据用户 id 分页获取点赞的帖子（返回 PostVO 分页）
     */
    Page<PostVO> getThumbPostVOPage(PostQueryRequest req);
}


