package com.wlx.ojbackendpostservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.BaseResponse;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ErrorCode;
import com.wlx.ojbackendcommon.common.ResultUtils;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.vo.PostVO;
import com.wlx.ojbackendpostservice.service.PostThumbService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thumb")
public class PostThumbController {

    @Resource
    private PostThumbService postThumbService;
    @Resource
    private UserFeignClient userFeignClient;

    @PostMapping("/save")
    @Operation(summary = "点赞帖子")
    public BaseResponse<Boolean> save(@RequestBody @Parameter(description = "帖子 id") DeleteRequest req,
                                      HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request).getId();
        boolean result = postThumbService.addThumb(req.getId(), userId);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "取消点赞")
    public BaseResponse<Boolean> remove(@RequestBody DeleteRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request).getId();
        boolean result = postThumbService.removeThumb(req.getId(), userId);
        return ResultUtils.success(result);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询用户点赞的帖子")
    public BaseResponse<Page<PostVO>> page(@RequestBody PostQueryRequest req, HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从 session 中获取当前登录用户，忽略客户端传入的 userId（不可信）
        long userId = userFeignClient.getLoginUser(request).getId();
        req.setUserId(userId);
        Page<PostVO> page = postThumbService.getThumbPostVOPage(req);
        return ResultUtils.success(page);
    }
}


