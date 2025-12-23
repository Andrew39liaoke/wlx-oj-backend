package com.wlx.ojbackendpostservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.entity.PostFavour;
import com.wlx.ojbackendmodel.model.vo.PostVO;

public interface PostFavourService extends IService<PostFavour> {

    /**
     * 收藏帖子
     */
    boolean addFavour(Long postId, Long userId);

    /**
     * 取消收藏
     */
    boolean removeFavour(Long postId, Long userId);

    /**
     * 根据用户 id 分页获取收藏的帖子（返回 PostVO 分页）
     */
    Page<PostVO> getFavourPostVOPage(PostQueryRequest req);
}


