package com.wlx.ojbackendpostservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.BaseResponse;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ErrorCode;
import com.wlx.ojbackendcommon.common.ResultUtils;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.post.PostAddRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostUpdateRequest;
import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.PostVO;
import com.wlx.ojbackendpostservice.service.PostService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class PostController {
    @Resource
    private PostService postService;
    @Resource
    private UserFeignClient userFeignClient;
    /**
     * 添加帖子。
     * @param req 帖子
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "保存帖子")
    public BaseResponse<Long> save(@Valid @RequestBody @Parameter(description = "帖子") PostAddRequest req,
                                      HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        long newPostId = postService.createPost(req, loginUser.getId());
        return ResultUtils.success(newPostId);
    }

    /**
     * 根据主键删除帖子。
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/remove/{id}")
    @Operation(summary = "删除主键帖子")
    public BaseResponse<Boolean> remove(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        boolean b = postService.deletePost(id, user.getId());
        return ResultUtils.success(b);
    }

    /**
     * 根据主键更新帖子。
     * @param req 帖子
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/update")
    @Operation(summary = "根据主键更新帖子")
    public BaseResponse<Boolean> update(@Valid @RequestBody @Parameter(description = "帖子") PostUpdateRequest req,
                                        HttpServletRequest request) {
        if (req == null || req.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        boolean result = postService.updatePost(req, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据帖子主键获取详细信息。
     * @param id 帖子主键
     * @return 帖子详情
     */
    @GetMapping("/getInfo/{id}")
    @Operation(summary = "根据主键获取帖子")
    public BaseResponse<PostVO> getInfo(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post post = postService.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(PostVO.objToVo(post));
    }

    /**
     * 分页查询帖子。
     * @return 分页对象
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询帖子")
    public BaseResponse<Page<PostVO>> page(@RequestBody PostQueryRequest req) {
        return ResultUtils.success(postService.getPostVOPage(req));
    }

    /**
     * 分页查询帖子。
     * @return 分页对象
     */
    @PostMapping("/pageSelf")
    @Operation(summary = "分页查询自己的帖子")
    public BaseResponse<Page<PostVO>> pageSelf(@RequestBody PostQueryRequest req, HttpServletRequest request) {
        User loginUser = userFeignClient.getLoginUser(request);
        return ResultUtils.success(postService.getMyPostVOPage(req, loginUser));
    }

}
