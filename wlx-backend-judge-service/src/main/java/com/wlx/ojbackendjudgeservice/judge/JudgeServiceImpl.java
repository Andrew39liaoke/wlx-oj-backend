package com.wlx.ojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.wlx.ojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.wlx.ojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.wlx.ojbackendjudgeservice.judge.strategy.JudgeContext;
import com.wlx.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.wlx.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.wlx.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.wlx.ojbackendmodel.model.dto.question.JudgeCase;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "题目正在判题中");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 4）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        JudgeInfo judgeInfo;
        try {
            ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
            List<String> outputList = executeCodeResponse.getOutputList();
            // 5）根据沙箱的执行结果，设置题目的判题状态和信息
            Integer sandboxStatus = executeCodeResponse.getStatus();
            // 沙箱执行出错（status=3），直接根据沙箱返回的错误信息设置状态
            if (sandboxStatus != null && sandboxStatus == 3) {
                judgeInfo = executeCodeResponse.getJudgeInfo();
                if (judgeInfo == null) {
                    judgeInfo = new JudgeInfo();
                }
                // 根据沙箱返回的错误信息确定具体的状态
                String sandboxMsg = executeCodeResponse.getMessage();
                if (sandboxMsg != null && (sandboxMsg.contains("编译出错") || sandboxMsg.contains("编译错误") || sandboxMsg.contains("编译"))) {
                    judgeInfo.setMessage("编译出错");
                } else if (sandboxMsg != null && sandboxMsg.contains("超出时间限制")) {
                    judgeInfo.setMessage("超出时间限制");
                } else if (sandboxMsg != null && sandboxMsg.contains("执行出错")) {
                    judgeInfo.setMessage("执行出错");
                } else {
                    judgeInfo.setMessage("内部出错");
                }
            } else {
                // 沙箱执行成功，走判题策略比较输出
                JudgeContext judgeContext = new JudgeContext();
                judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
                judgeContext.setInputList(inputList);
                judgeContext.setOutputList(outputList);
                judgeContext.setJudgeCaseList(judgeCaseList);
                judgeContext.setQuestion(question);
                judgeContext.setQuestionSubmit(questionSubmit);
                judgeInfo = judgeManager.doJudge(judgeContext);
            }
        } catch (Exception e) {
            // 沙箱调用异常，根据异常信息尝试识别错误类型
            log.error("判题过程异常: ", e);
            judgeInfo = new JudgeInfo();
            String errMsg = e.getMessage();
            if (errMsg != null && (errMsg.contains("编译") || errMsg.contains("Compile"))) {
                judgeInfo.setMessage("编译出错");
            } else if (errMsg != null && errMsg.contains("超时")) {
                judgeInfo.setMessage("超出时间限制");
            } else {
                judgeInfo.setMessage("内部出错");
            }
        }
        // 6）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        // 根据判题消息映射到对应的提交状态
        QuestionSubmitStatusEnum statusEnum = QuestionSubmitStatusEnum.getEnumByText(judgeInfo.getMessage());
        questionSubmitUpdate.setStatus(
                statusEnum != null ? statusEnum.getValue() : QuestionSubmitStatusEnum.SYSTEM_ERROR.getValue()
        );
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        questionSubmitUpdate.setPassCaseCount(judgeInfo.getPassCaseCount());
        questionSubmitUpdate.setTotalCaseCount(judgeInfo.getTotalCaseCount());
        questionSubmitUpdate.setPassRate(judgeInfo.getPassRate());
        questionSubmitUpdate.setScore(judgeInfo.getScore());
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "题目状态更新错误");
        }

        // 如果判题结果为通过，则增加题目的通过数
        if (statusEnum != null && QuestionSubmitStatusEnum.ACCEPTED.getValue().equals(statusEnum.getValue())) {
            questionFeignClient.incrementAcceptedNum(questionId);
        }

        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionId);
        return questionSubmitResult;
    }
}
