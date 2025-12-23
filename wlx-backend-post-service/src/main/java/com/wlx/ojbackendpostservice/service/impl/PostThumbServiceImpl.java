package com.wlx.ojbackendpostservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendcommon.common.ErrorCode;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.PostThumb;
import com.wlx.ojbackendmodel.model.vo.PostVO;
import com.wlx.ojbackendpostservice.mapper.PostThumbMapper;
import com.wlx.ojbackendpostservice.service.PostThumbService;
import com.wlx.ojbackendpostservice.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostThumbServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb> implements PostThumbService {

    @Resource
    private PostService postService;

    @Override
    public boolean addThumb(Long postId, Long userId) {
        ThrowUtils.throwIf(postId == null || postId <= 0 || userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        // 已经点赞则幂等返回 true
        PostThumb exist = this.lambdaQuery()
                .eq(PostThumb::getPostId, postId)
                .eq(PostThumb::getUserId, userId)
                .one();
        if (exist != null) {
            return true;
        }
        Post post = postService.getById(postId);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        PostThumb thumb = new PostThumb();
        thumb.setPostId(postId);
        thumb.setUserId(userId);
        thumb.setCreateTime(new Date());
        boolean saveResult = this.save(thumb);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        // 更新帖子点赞计数
        Integer thumbNum = post.getThumbNum() == null ? 0 : post.getThumbNum();
        post.setThumbNum(thumbNum + 1);
        boolean updateResult = postService.updateById(post);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean removeThumb(Long postId, Long userId) {
        ThrowUtils.throwIf(postId == null || postId <= 0 || userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        PostThumb exist = this.lambdaQuery()
                .eq(PostThumb::getPostId, postId)
                .eq(PostThumb::getUserId, userId)
                .one();
        if (exist == null) {
            return true;
        }
        boolean removeResult = this.removeById(exist.getId());
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
        Post post = postService.getById(postId);
        if (post != null) {
            Integer thumbNum = post.getThumbNum() == null ? 0 : post.getThumbNum();
            post.setThumbNum(Math.max(0, thumbNum - 1));
            postService.updateById(post);
        }
        return true;
    }

    @Override
    public Page<PostVO> getThumbPostVOPage(PostQueryRequest req) {
        ThrowUtils.throwIf(req == null || req.getUserId() == null || req.getUserId() <= 0, ErrorCode.PARAMS_ERROR);
        Page<PostThumb> page = new Page<>((int) req.getCurrent(), (int) req.getPageSize());
        QueryWrapper<PostThumb> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", req.getUserId()).orderByDesc("create_time");
        this.page(page, wrapper);
        List<PostThumb> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }
        List<Long> postIds = records.stream().map(PostThumb::getPostId).collect(Collectors.toList());
        List<Post> posts = postService.listByIds(postIds);
        // 保证返回顺序与点赞顺序一致（按点赞时间倒序）
        List<PostVO> postVOs = new ArrayList<>();
        for (PostThumb pf : records) {
            for (Post p : posts) {
                if (p.getId().equals(pf.getPostId())) {
                    postVOs.add(PostVO.objToVo(p));
                    break;
                }
            }
        }
        Page<PostVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(postVOs);
        return result;
    }
}


