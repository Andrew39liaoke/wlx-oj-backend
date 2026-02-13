package com.wlx.ojbackendaiservice.controller;

import com.wlx.ojbackendaiservice.service.CodeEvaluationAgentService;
import com.wlx.ojbackendcommon.common.MessageRequest;
import com.wlx.ojbackendmodel.model.dto.ai.ChatMessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 代码评估Controller
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/")
public class CodeEvaluationController {
    @Autowired
    private CodeEvaluationAgentService codeEvaluationAgentService;


    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;
    /**
     * 提交代码进行评估（流式返回）- POST版本，使用 @RequestBody 接收 JSON
     * 请求示例：
     * POST http://localhost:8088/evaluate
     * Content-Type: application/json
     * {
     *   "userCode": "你的代码内容",
     *   "userId": 1
     * }
     */
    @PostMapping(value = "/evaluate")
    public Flux<String> evaluateCodePost(@RequestBody MessageRequest messageRequest) {
        String message = messageRequest.getMsg();
        Long userId = messageRequest.getUserId();

        log.info("收到代码评估请求（POST），用户ID: {}", userId);
        // 参数校验
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("错误：代码不能为空");
        }
        // 调用服务进行流式评估
        return codeEvaluationAgentService.evaluateCodeStream(message, String.valueOf(userId));
    }

    /**
     * 使用向量数据库查询后进行流式代码评估
     * 请求示例：
     * POST http://localhost:8088/evaluate
     * Content-Type: application/json
     * {
     *   "msg": "请帮我评估这段代码：public int add(int a, int b) { return a + b; }",
     *   "userId": 1
     * }
     *
     * 或带参数：
     * POST http://localhost:8088/evaluate-with-rag?topK=3&threshold=0.8
     * Content-Type: application/json
     * {
     *   "msg": "请帮我评估这段代码：public int add(int a, int b) { return a + b; }",
     *   "userId": 1
     * }
     */
    @PostMapping(value = "/evaluate/rag")
    public Flux<String> evaluateCodeWithRag(
            @RequestBody MessageRequest messageRequest,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Double threshold) {

        String message = messageRequest.getMsg();
        Long userId = messageRequest.getUserId();

        log.info("收到RAG代码评估请求，用户ID: {}, topK: {}, threshold: {}", userId, topK, threshold);

        // 参数校验
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("错误：消息内容不能为空");
        }

        if (userId == null) {
            return Flux.just("错误：用户ID不能为空");
        }

        // 调用服务进行向量搜索+流式评估
        return codeEvaluationAgentService.evaluateCodeWithVectorSearch(message, String.valueOf(userId), topK, threshold);
    }
    /**
     * 获取用户的聊天历史记录
     */
    @GetMapping("/chat/history")
    public List<ChatMessageDTO> getChatHistory(@RequestParam Long userId) {
        log.info("获取用户聊天历史，用户ID: {}", userId);
        return codeEvaluationAgentService.getChatHistory(String.valueOf(userId));
    }
}
