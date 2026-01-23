package com.wlx.ojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionThumb;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendquestionservice.mapper.QuestionThumbMapper;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import com.wlx.ojbackendquestionservice.service.QuestionThumbService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Service
public class QuestionThumbServiceImpl extends ServiceImpl<QuestionThumbMapper, QuestionThumb> implements QuestionThumbService {

    @Resource
    private QuestionService questionService;

    @Override
    public boolean addThumb(Long questionId, Long userId) {
        ThrowUtils.throwIf(questionId == null || questionId <= 0 || userId == null || userId <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        // 已经点赞则幂等返回 true
        QuestionThumb exist = this.lambdaQuery()
                .eq(QuestionThumb::getQuestionId, questionId)
                .eq(QuestionThumb::getUserId, userId)
                .one();
        if (exist != null) {
            return true;
        }
        Question question = questionService.getById(questionId);
        ThrowUtils.throwIf(question == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        QuestionThumb thumb = new QuestionThumb();
        thumb.setQuestionId(questionId);
        thumb.setUserId(userId);
        thumb.setCreateTime(new Date());
        boolean saveResult = this.save(thumb);
        ThrowUtils.throwIf(!saveResult, ResopnseCodeEnum.OPERATION_ERROR);
        // 更新题目点赞计数
        Integer thumbNum = question.getThumbNum() == null ? 0 : question.getThumbNum();
        question.setThumbNum(thumbNum + 1);
        boolean updateResult = questionService.updateById(question);
        ThrowUtils.throwIf(!updateResult, ResopnseCodeEnum.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean removeThumb(Long questionId, Long userId) {
        ThrowUtils.throwIf(questionId == null || questionId <= 0 || userId == null || userId <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        QuestionThumb exist = this.lambdaQuery()
                .eq(QuestionThumb::getQuestionId, questionId)
                .eq(QuestionThumb::getUserId, userId)
                .one();
        if (exist == null) {
            return true;
        }
        boolean removeResult = this.removeById(exist.getId());
        ThrowUtils.throwIf(!removeResult, ResopnseCodeEnum.OPERATION_ERROR);
        Question question = questionService.getById(questionId);
        if (question != null) {
            Integer thumbNum = question.getThumbNum() == null ? 0 : question.getThumbNum();
            question.setThumbNum(Math.max(0, thumbNum - 1));
            questionService.updateById(question);
        }
        return true;
    }

    @Override
    public Page<QuestionVO> getThumbQuestionVOPage(QuestionQueryRequest req) {
        ThrowUtils.throwIf(req == null || req.getUserId() == null || req.getUserId() <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        Page<QuestionThumb> page = new Page<>((int)req.getCurrent(), (int)req.getPageSize());
        QueryWrapper<QuestionThumb> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", req.getUserId()).orderByDesc("create_time");
        this.page(page, wrapper);
        List<QuestionThumb> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }
        List<Long> questionIds = records.stream().map(QuestionThumb::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionService.listByIds(questionIds);
        // 保证返回顺序与点赞顺序一致（按点赞时间倒序）
        List<QuestionVO> questionVOs = new ArrayList<>();
        for (QuestionThumb qt : records) {
            for (Question q : questions) {
                if (q.getId().equals(qt.getQuestionId())) {
                    questionVOs.add(questionService.getQuestionVO(q));
                    break;
                }
            }
        }
        Page<QuestionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(questionVOs);
        return result;
    }

    @Override
    public List<Long> getThumbStatus(List<Long> questionIds, Long userId) {
        if (questionIds == null || questionIds.isEmpty() || userId == null || userId <= 0) {
            return new ArrayList<>();
        }
        List<QuestionThumb> thumbs = this.lambdaQuery()
                .eq(QuestionThumb::getUserId, userId)
                .in(QuestionThumb::getQuestionId, questionIds)
                .list();
        return thumbs.stream()
                .map(QuestionThumb::getQuestionId)
                .collect(Collectors.toList());
    }
}
