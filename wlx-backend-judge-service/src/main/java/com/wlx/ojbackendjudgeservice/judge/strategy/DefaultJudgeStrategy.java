package com.wlx.ojbackendjudgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.wlx.ojbackendmodel.model.dto.question.JudgeCase;
import com.wlx.ojbackendmodel.model.dto.question.JudgeConfig;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

import java.util.List;

/**
 * 默认判题策略
 */
public class DefaultJudgeStrategy implements JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 计算通过测试用例数、总数、通过率和得分
        int passCount = 0;
        int maxCompareCount = Math.min(outputList.size(), judgeCaseList.size());
        for (int i = 0; i < maxCompareCount; i++) {
            if (judgeCaseList.get(i).getOutput().equals(outputList.get(i))) {
                passCount++;
            }
        }
        judgeInfoResponse.setPassCaseCount(passCount);
        judgeInfoResponse.setTotalCaseCount(judgeCaseList.size());
        double passRate = judgeCaseList.size() == 0 ? 0 : (double) passCount / judgeCaseList.size();
        judgeInfoResponse.setPassRate(passRate);
        judgeInfoResponse.setScore(passRate * 100.0);

        // 获取题目限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long needMemoryLimit = judgeConfig.getMemoryLimit();
        Long needTimeLimit = judgeConfig.getTimeLimit();
        // 先判断时间限制（超时会导致输出为空，必须优先检查）
        if (time > needTimeLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        // 判断内存限制
        if (memory > needMemoryLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        // 判断是否完全正确（输出数量是否和预期相等，且全部通过）
        if (outputList.size() != inputList.size() || passCount != judgeCaseList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }

        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }
}
