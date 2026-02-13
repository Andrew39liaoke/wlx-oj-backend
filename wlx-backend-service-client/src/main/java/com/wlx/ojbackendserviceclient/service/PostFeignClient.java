package com.wlx.ojbackendserviceclient.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}
