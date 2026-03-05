package com.wlx.ojbackendserviceclient.service;

import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import com.wlx.ojbackendmodel.model.entity.PostFavour;
import com.wlx.ojbackendmodel.model.entity.PostThumb;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * 帖子服务
 */
@FeignClient(name = "wlx-backend-post-service", path = "/api/post/inner")
public interface PostFeignClient {

    /**
     * 更新帖子封面
     *
     * @param postId 帖子ID
     * @param coverUrl 封面URL
     * @return 是否更新成功
     */
    @PostMapping("/updateCover")
    boolean updatePostCover(@RequestParam("postId") Long postId, @RequestParam("coverUrl") String coverUrl);

    /**
     * 获取所有帖子点赞记录
     *
     * @return 帖子点赞记录列表
     */
    @GetMapping("/thumb/listAll")
    List<PostThumb> listAllPostThumbs();

    /**
     * 获取所有帖子收藏记录
     *
     * @return 帖子收藏记录列表
     */
    @GetMapping("/favour/listAll")
    List<PostFavour> listAllPostFavours();

    /**
     * 获取所有帖子评论记录
     *
     * @return 帖子评论记录列表
     */
    @GetMapping("/comment/listAll")
    List<PostComment> listAllPostComments();

    /**
     * 根据帖子ID列表批量查询帖子信息
     *
     * @param postIds 帖子ID列表
     * @return 帖子列表
     */
    @GetMapping("/list/byIds")
    List<Post> listPostsByIds(@RequestParam("postIds") Collection<Long> postIds);

    /**
     * 获取所有未删除帖子列表
     *
     * @return 帖子列表
     */
    @GetMapping("/list/all")
    List<Post> listAllPosts();
}
