package com.wlx.ojbackendpostservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.post.PostAddRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostQueryRequest;
import com.wlx.ojbackendmodel.model.dto.post.PostUpdateRequest;
import com.wlx.ojbackendmodel.model.entity.Post;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.PostVO;
import com.wlx.ojbackendpostservice.mapper.PostMapper;
import com.wlx.ojbackendpostservice.service.PostService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final static Gson GSON = new Gson();

    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public Page<PostVO> getPostVOPage(PostQueryRequest postQueryRequest) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        QueryWrapper<Post> wrapper = new QueryWrapper<>();

        // 联合模糊查询：标题、分区、标签
        String title = postQueryRequest.getTitle();
        String zone = postQueryRequest.getZone();
        List<String> tags = postQueryRequest.getTags();

        // 如果有查询条件，则构建联合查询
        if (StringUtils.isNotBlank(title) || StringUtils.isNotBlank(zone) ||
            (tags != null && !tags.isEmpty())) {
            wrapper.and(w -> {
                // 标题模糊查询
                w.like(StringUtils.isNotBlank(title), "title", title);
                // 分区模糊查询
                w.like(StringUtils.isNotBlank(zone), "zone", zone);
                // 标签模糊查询（JSON数组匹配）
                if (tags != null && !tags.isEmpty()) {
                    for (String tag : tags) {
                        if (StringUtils.isNotBlank(tag)) {
                            // 使用 LIKE 来进行模糊匹配，支持部分匹配
                            w.like("tags", tag);
                        }
                    }
                }
            });
        }

        Page<Post> postPage = this.page(new Page<>(current, size), wrapper);

        // 获取所有用户ID
        Set<Long> userIdSet = postPage.getRecords().stream().map(Post::getUserId).collect(Collectors.toSet());
        // 批量查询用户信息
        Map<Long, User> userIdUserMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 转换 PostVO 并设置用户信息
        List<PostVO> postVOList = postPage.getRecords().stream().map(post -> {
            PostVO postVO = PostVO.objToVo(post);
            Long userId = post.getUserId();
            if (userId != null && userIdUserMap.containsKey(userId)) {
                User user = userIdUserMap.get(userId);
                postVO.setUser(userFeignClient.getUserVO(user));
            }
            return postVO;
        }).collect(Collectors.toList());

        Page<PostVO> page = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        page.setRecords(postVOList);
        return page;
    }

    @Override
    public Page<PostVO> getMyPostVOPage(PostQueryRequest postQueryRequest, User loginUser) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", loginUser.getId());
        wrapper.like(StringUtils.isNotBlank(postQueryRequest.getTitle()), "title", postQueryRequest.getTitle());
        Page<Post> postPage = this.page(new Page<>(current, size), wrapper);
        List<PostVO> postVOList = postPage.getRecords().stream().map(PostVO::objToVo).collect(Collectors.toList());
        Page<PostVO> page = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        page.setRecords(postVOList);
        return page;
    }

    @Override
    public long createPost(PostAddRequest postAddRequest, long userId) {
        Post post = new Post();
        BeanUtils.copyProperties(postAddRequest, post);
        List<String> tags = postAddRequest.getTags();
        if (tags != null) {
            post.setTags(GSON.toJson(tags));
        }
        post.setUserId(userId);
        post.setThumbNum(0);
        post.setFavourNum(0);
        boolean result = this.save(post);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return post.getId();
    }

    @Override
    public boolean deletePost(long id, long userId) {
        Post post = this.getById(id);
        ThrowUtils.throwIf(post == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!post.getUserId().equals(userId), ResopnseCodeEnum.NO_AUTH_ERROR);
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return result;
    }

    @Override
    public boolean updatePost(PostUpdateRequest postUpdateRequest, long userId) {
        Post exist = this.getById(postUpdateRequest.getId());
        ThrowUtils.throwIf(exist == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!exist.getUserId().equals(userId), ResopnseCodeEnum.NO_AUTH_ERROR);
        Post post = new Post();
        BeanUtils.copyProperties(postUpdateRequest, post);
        post.setUserId(exist.getUserId());
        List<String> tags = postUpdateRequest.getTags();
        if (tags != null) {
            post.setTags(GSON.toJson(tags));
        }
        boolean result = this.updateById(post);
        ThrowUtils.throwIf(!result, ResopnseCodeEnum.OPERATION_ERROR);
        return result;
    }
}


