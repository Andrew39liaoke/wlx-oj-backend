package com.wlx.ojbackendaiservice.tools;

import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProblemSolutionTools {
    @Resource
    private QuestionFeignClient questionFeignClient;

    @Tool(description = "获取题目的解答过程")
    public String getAnswer(@ToolParam(description = "题目id") Long problemId) {
        String answer = questionFeignClient.getQuestionAnswer(problemId);
        if (answer != null && !answer.isEmpty()) {
            return answer;
        }
        return "未发现解答过程";
    }
}
