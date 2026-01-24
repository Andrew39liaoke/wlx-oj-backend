package com.wlx.ojbackendpostservice.controller;

import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.postComment.PostCommentRequest;
import com.wlx.ojbackendmodel.model.entity.PostComment;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.PostCommentVO;
import com.wlx.ojbackendpostservice.service.PostCommentService;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    public ResponseEntity<Long> save(@Valid @RequestBody @Parameter(description = "评论") PostCommentRequest req,
                                     HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        long newId = postCommentService.createComment(req, userId);
        return Result.success(newId);
    }

    @DeleteMapping("/remove")
    @Operation(summary = "删除评论")
    public ResponseEntity<Boolean> remove(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = postCommentService.deleteComment(deleteRequest.getId(), userId);
        return Result.success(result);
    }

    @PutMapping("/update")
    @Operation(summary = "更新评论")
    public ResponseEntity<Boolean> update(@Valid @RequestBody @Parameter(description = "评论") PostCommentRequest req,
                                          HttpServletRequest request) {
        if (req == null || req.getId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        long userId = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER)).getId();
        boolean result = postCommentService.updateComment(req, userId);
        return Result.success(result);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取评论")
    public ResponseEntity<PostCommentVO> getInfo(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        PostComment comment = postCommentService.getById(id);
        if (comment == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        return Result.success(PostCommentVO.objToVo(comment));
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "获取帖子的评论列表")
    public ResponseEntity<List<PostCommentVO>> getPostComments(@PathVariable Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        List<PostComment> comments = postCommentService.getCommentsByPostId(postId);

        // 获取所有用户ID
        Set<Long> userIdSet = comments.stream()
                .map(PostComment::getUserId)
                .collect(Collectors.toSet());

        // 批量查询用户信息
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 转换 PostCommentVO 并设置用户信息
        List<PostCommentVO> commentVOs = comments.stream()
                .map(comment -> {
                    PostCommentVO commentVO = PostCommentVO.objToVo(comment);
                    Long userId = comment.getUserId();
                    if (userId != null && userIdUserListMap.containsKey(userId)) {
                        User user = userIdUserListMap.get(userId).get(0);
                        commentVO.setUserVO(userFeignClient.getUserVO(user));
                    }
                    return commentVO;
                })
                .toList();
        return Result.success(commentVOs);
    }
}


