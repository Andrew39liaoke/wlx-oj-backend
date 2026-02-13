package com.wlx.ojbackendpostservice.controller.inner;

import com.wlx.ojbackendserviceclient.service.PostFeignClient;
import com.wlx.ojbackendpostservice.service.PostService;
import com.wlx.ojbackendmodel.model.entity.Post;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class PostInnerController implements PostFeignClient {

    @Resource
    private PostService postService;

    /**
     * 更新帖子封面
     *
     * @param postId 帖子ID
     * @param coverUrl 封面URL
     * @return 是否更新成功
     */
    @Override
    @PostMapping("/updateCover")
    public boolean updatePostCover(@RequestParam("postId") Long postId, @RequestParam("coverUrl") String coverUrl) {
        Post post = new Post();
        post.setId(postId);
        post.setCover(coverUrl);
        return postService.updateById(post);
    }
}
