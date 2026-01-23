package com.wlx.ojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionFavour;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendquestionservice.mapper.QuestionFavourMapper;
import com.wlx.ojbackendquestionservice.service.QuestionFavourService;
import com.wlx.ojbackendquestionservice.service.QuestionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Service
public class QuestionFavourServiceImpl extends ServiceImpl<QuestionFavourMapper, QuestionFavour> implements QuestionFavourService {

    @Resource
    private QuestionService questionService;

    @Override
    public boolean addFavour(Long questionId, Long userId) {
        ThrowUtils.throwIf(questionId == null || questionId <= 0 || userId == null || userId <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        // 已经收藏则幂等返回 true
        QuestionFavour exist = this.lambdaQuery()
                .eq(QuestionFavour::getQuestionId, questionId)
                .eq(QuestionFavour::getUserId, userId)
                .one();
        if (exist != null) {
            return true;
        }
        Question question = questionService.getById(questionId);
        ThrowUtils.throwIf(question == null, ResopnseCodeEnum.NOT_FOUND_ERROR);
        QuestionFavour favour = new QuestionFavour();
        favour.setQuestionId(questionId);
        favour.setUserId(userId);
        favour.setCreateTime(new Date());
        boolean saveResult = this.save(favour);
        ThrowUtils.throwIf(!saveResult, ResopnseCodeEnum.OPERATION_ERROR);
        // 更新题目收藏计数
        Integer favourNum = question.getFavourNum() == null ? 0 : question.getFavourNum();
        question.setFavourNum(favourNum + 1);
        boolean updateResult = questionService.updateById(question);
        ThrowUtils.throwIf(!updateResult, ResopnseCodeEnum.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean removeFavour(Long questionId, Long userId) {
        ThrowUtils.throwIf(questionId == null || questionId <= 0 || userId == null || userId <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        QuestionFavour exist = this.lambdaQuery()
                .eq(QuestionFavour::getQuestionId, questionId)
                .eq(QuestionFavour::getUserId, userId)
                .one();
        if (exist == null) {
            return true;
        }
        boolean removeResult = this.removeById(exist.getId());
        ThrowUtils.throwIf(!removeResult, ResopnseCodeEnum.OPERATION_ERROR);
        Question question = questionService.getById(questionId);
        if (question != null) {
            Integer favourNum = question.getFavourNum() == null ? 0 : question.getFavourNum();
            question.setFavourNum(Math.max(0, favourNum - 1));
            questionService.updateById(question);
        }
        return true;
    }

    @Override
    public Page<QuestionVO> getFavourQuestionVOPage(QuestionQueryRequest req) {
        ThrowUtils.throwIf(req == null || req.getUserId() == null || req.getUserId() <= 0, ResopnseCodeEnum.PARAMS_ERROR);
        Page<QuestionFavour> page = new Page<>((int)req.getCurrent(), (int)req.getPageSize());
        QueryWrapper<QuestionFavour> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", req.getUserId()).orderByDesc("create_time");
        this.page(page, wrapper);
        List<QuestionFavour> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }
        List<Long> questionIds = records.stream().map(QuestionFavour::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionService.listByIds(questionIds);
        // 保证返回顺序与收藏顺序一致（按收藏时间倒序）
        List<QuestionVO> questionVOs = new ArrayList<>();
        for (QuestionFavour qf : records) {
            for (Question q : questions) {
                if (q.getId().equals(qf.getQuestionId())) {
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
    public List<Long> getFavourStatus(List<Long> questionIds, Long userId) {
        if (questionIds == null || questionIds.isEmpty() || userId == null || userId <= 0) {
            return new ArrayList<>();
        }
        List<QuestionFavour> favours = this.lambdaQuery()
                .eq(QuestionFavour::getUserId, userId)
                .in(QuestionFavour::getQuestionId, questionIds)
                .list();
        return favours.stream()
                .map(QuestionFavour::getQuestionId)
                .collect(Collectors.toList());
    }
}
