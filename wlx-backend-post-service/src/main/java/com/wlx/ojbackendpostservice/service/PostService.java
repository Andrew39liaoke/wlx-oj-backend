package com.wlx.ojbackendpostservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.post.PostAddRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostUpdateRequest;
import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.PostVO;

public interface PostService extends IService<Post> {

    /**
     * 分页获取帖子封装
     *
     * @param postQueryRequest
     * @return
     */
    Page<PostVO> getPostVOPage(PostQueryRequest postQueryRequest);

    /**
     * 分页获取当前用户的帖子
     *
     * @param postQueryRequest
     * @param loginUser
     * @return
     */
    Page<PostVO> getMyPostVOPage(PostQueryRequest postQueryRequest, User loginUser);
    
    /**
     * 创建帖子，返回新帖 id
     *
     * @param postAddRequest
     * @param userId
     * @return
     */
    long createPost(PostAddRequest postAddRequest, long userId);
    
    /**
     * 根据 id 删除帖子，仅允许作者删除
     *
     * @param id
     * @param userId
     * @return
     */
    boolean deletePost(long id, long userId);

    /**
     * 更新帖子，仅允许作者更新
     *
     * @param postUpdateRequest
     * @param userId
     * @return
     */
    boolean updatePost(PostUpdateRequest postUpdateRequest, long userId);
}
