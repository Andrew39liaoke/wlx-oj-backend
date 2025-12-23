package com.wlx.ojbackendpostservice.controller;

import com.wlx.ojbackendcommon.common.BaseResponse;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ErrorCode;
import com.wlx.ojbackendcommon.common.ResultUtils;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.postComment.PostCommentRequest;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import com.wlx.ojbackendmodel.model.vo.PostCommentVO;
import com.wlx.ojbackendpostservice.service.PostCommentService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
public class PostCommentController {

    @Resource
    private PostCommentService postCommentService;
    @Resource
    private UserFeignClient userFeignClient;

    @PostMapping("/save")
    @Operation(summary = "保存评论")
    public BaseResponse<Long> save(@Valid @RequestBody @Parameter(description = "评论") PostCommentRequest req,
                                   HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request).getId();
        long newId = postCommentService.createComment(req, userId);
        return ResultUtils.success(newId);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "删除评论")
    public BaseResponse<Boolean> remove(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request).getId();
        boolean result = postCommentService.deleteComment(deleteRequest.getId(), userId);
        return ResultUtils.success(result);
    }

    @PutMapping("/update")
    @Operation(summary = "更新评论")
    public BaseResponse<Boolean> update(@Valid @RequestBody @Parameter(description = "评论") PostCommentRequest req,
                                        HttpServletRequest request) {
        if (req == null || req.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request).getId();
        boolean result = postCommentService.updateComment(req, userId);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取评论")
    public BaseResponse<PostCommentVO> getInfo(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PostComment comment = postCommentService.getById(id);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(PostCommentVO.objToVo(comment));
    }
}


