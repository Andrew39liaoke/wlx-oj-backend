package com.wlx.ojbackendpostservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.vo.PostVO;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendpostservice.service.PostFavourService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favour")
public class PostFavourController {

    @Resource
    private PostFavourService postFavourService;
    @Resource
    private UserFeignClient userFeignClient;

    @PostMapping("/save")
    @Operation(summary = "收藏帖子")
    public ResponseEntity<Boolean> save(@RequestBody @Parameter(description = "帖子 id") DeleteRequest req,
                                        HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = postFavourService.addFavour(req.getId(), userId);
        return Result.success(result);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "取消收藏")
    public ResponseEntity<Boolean> remove(@RequestBody DeleteRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = postFavourService.removeFavour(req.getId(), userId);
        return Result.success(result);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询用户收藏的帖子")
    public ResponseEntity<Page<PostVO>> page(@RequestBody PostQueryRequest req, HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        req.setUserId(userId);
        Page<PostVO> page = postFavourService.getFavourPostVOPage(req);
        return Result.success(page);
    }
}


