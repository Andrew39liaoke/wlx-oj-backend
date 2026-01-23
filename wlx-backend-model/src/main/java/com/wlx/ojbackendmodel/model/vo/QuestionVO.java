package com.wlx.ojbackendmodel.model.vo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendmodel.model.dto.question.JudgeCase;
import com.wlx.ojbackendmodel.model.dto.question.JudgeConfig;
import com.wlx.ojbackendmodel.model.entity.Question;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 题目封装类
 * @TableName question
 */
@Data
public class QuestionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

    /**
     * 判题配置（json 对象）
     */
    private JudgeConfig judgeConfig;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建题目人的信息
     */
    private UserVO userVO;

    /**
     * 判题用例
     */
    private List<JudgeCase> judgeCase;

    /**
     * 包装类转对象
     *
     * @param questionVO
     * @return
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);
        List<String> tagList = questionVO.getTags();
        if (tagList != null) {
            question.setTags(JSONUtil.toJsonStr(tagList));
        }
        JudgeConfig voJudgeConfig = questionVO.getJudgeConfig();
        if (voJudgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(voJudgeConfig));
        }
        List<JudgeCase> voJudgeCase = questionVO.getJudgeCase();
        if (voJudgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(voJudgeCase));
        }
        return question;
    }

    /**
     * 对象转包装类
     *
     * @param question
     * @return
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO);
        List<String> tagList = new ArrayList<>();
        if (question.getTags() != null && StrUtil.isNotBlank(question.getTags())) {
            tagList = JSONUtil.toList(question.getTags(), String.class);
        }
        questionVO.setTags(tagList);
        String judgeConfigStr = question.getJudgeConfig();
        if (judgeConfigStr != null && !judgeConfigStr.trim().isEmpty()) {
            questionVO.setJudgeConfig(JSONUtil.toBean(judgeConfigStr, JudgeConfig.class));
        }
        List<JudgeCase> judgeCaseList = new ArrayList<>();
        if (question.getJudgeCase() != null && StrUtil.isNotBlank(question.getJudgeCase())) {
            judgeCaseList = JSONUtil.toList(question.getJudgeCase(), JudgeCase.class);
        }
        questionVO.setJudgeCase(judgeCaseList);
        return questionVO;
    }

    private static final long serialVersionUID = 1L;
}