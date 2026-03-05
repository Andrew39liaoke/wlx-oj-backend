package com.wlx.ojbackendpostservice.controller.inner;

import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import com.wlx.ojbackendmodel.model.entity.PostFavour;
import com.wlx.ojbackendmodel.model.entity.PostThumb;
import com.wlx.ojbackendpostservice.service.PostCommentService;
import com.wlx.ojbackendpostservice.service.PostFavourService;
import com.wlx.ojbackendpostservice.service.PostService;
import com.wlx.ojbackendpostservice.service.PostThumbService;
import com.wlx.ojbackendserviceclient.service.PostFeignClient;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class PostInnerController implements PostFeignClient {

    @Resource
    private PostService postService;

    @Resource
    private PostThumbService postThumbService;

    @Resource
    private PostFavourService postFavourService;

    @Resource
    private PostCommentService postCommentService;

    /**
     * 更新帖子封面
     */
    @Override
    @PostMapping("/updateCover")
    public boolean updatePostCover(@RequestParam("postId") Long postId, @RequestParam("coverUrl") String coverUrl) {
        Post post = new Post();
        post.setId(postId);
        post.setCover(coverUrl);
        return postService.updateById(post);
    }

    /**
     * 获取所有帖子点赞记录
     */
    @Override
    @GetMapping("/thumb/listAll")
    public List<PostThumb> listAllPostThumbs() {
        return postThumbService.list();
    }

    /**
     * 获取所有帖子收藏记录
     */
    @Override
    @GetMapping("/favour/listAll")
    public List<PostFavour> listAllPostFavours() {
        return postFavourService.list();
    }

    /**
     * 获取所有帖子评论记录
     */
    @Override
    @GetMapping("/comment/listAll")
    public List<PostComment> listAllPostComments() {
        return postCommentService.list();
    }

    /**
     * 根据帖子ID列表批量查询帖子信息
     */
    @Override
    @GetMapping("/list/byIds")
    public List<Post> listPostsByIds(@RequestParam("postIds") Collection<Long> postIds) {
        return postService.listByIds(postIds);
    }

    /**
     * 获取所有未删除帖子列表
     */
    @Override
    @GetMapping("/list/all")
    public List<Post> listAllPosts() {
        return postService.list();
    }
}
