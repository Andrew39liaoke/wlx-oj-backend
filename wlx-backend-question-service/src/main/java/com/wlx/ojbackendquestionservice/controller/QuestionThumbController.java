package com.wlx.ojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendquestionservice.service.QuestionThumbService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question/thumb")
public class QuestionThumbController {

    @Resource
    private QuestionThumbService questionThumbService;
    @Resource
    private UserFeignClient userFeignClient;

    @PostMapping("/save")
    @Operation(summary = "点赞题目")
    public ResponseEntity<Boolean> save(@RequestBody @Parameter(description = "题目 id") DeleteRequest req,
                                        HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = questionThumbService.addThumb(req.getId(), userId);
        return Result.success(result);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "取消点赞")
    public ResponseEntity<Boolean> remove(@RequestBody DeleteRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = questionThumbService.removeThumb(req.getId(), userId);
        return Result.success(result);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询用户点赞的题目")
    public ResponseEntity<Page<QuestionVO>> page(@RequestBody QuestionQueryRequest req, HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        req.setUserId(userId);
        Page<QuestionVO> page = questionThumbService.getThumbQuestionVOPage(req);
        return Result.success(page);
    }
}
