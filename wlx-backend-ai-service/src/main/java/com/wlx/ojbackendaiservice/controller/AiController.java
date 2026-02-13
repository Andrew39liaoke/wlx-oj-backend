package com.wlx.ojbackendaiservice.controller;

import com.wlx.ojbackendaiservice.service.AIService;
import com.wlx.ojbackendaiservice.service.impl.AIServiceImpl;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendmodel.model.dto.ai.AiInputDTO;
import com.wlx.ojbackendmodel.model.dto.ai.AiType;
import com.wlx.ojbackendmodel.model.dto.ai.ChatMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/ai")
public class AiController {
    @Resource
    private AIService aiService;

    @Resource
    private AIServiceImpl aiServiceImpl;


    /**
     * 流式输出聊天内容的接口
     *
     * @param aiInputDTO Ai输入
     * @return Flux<ServerSentEvent < Object>>返回流式聊天内容
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "聊天")
    public Flux<ServerSentEvent<Object>> chat(@RequestBody @Valid AiInputDTO aiInputDTO) {
        return aiService.getChat(aiInputDTO);
    }

    @PostMapping(value = "/questionAnswer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "题目解答")
    public Flux<ServerSentEvent<Object>> questionAnswer(@RequestBody @Valid AiInputDTO aiInputDTO) {
        return aiService.questionAnswer(aiInputDTO);
    }

    /**
     * 生成题目的接口
     *
     * @param userId  用户ID
     * @param require 题目要求
     * @return ResponseEntity<Flux < String>>返回生成的题目
     */
    @GetMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generate(@RequestParam("userId") Long userId, String require) {
        return aiService.generateQuestion(require);
    }

    /**
     * 查询会话历史列表
     *
     * @param userId 用户ID
     * @param type 业务类型
     * @return chatId列表
     */
    @GetMapping("/history/{type}")
    @Operation(summary = "查询会话id列表")
    public ResponseEntity<List<String>> getChatIds(@RequestParam("userId") Long userId, @PathVariable("type") AiType type) {
        List<String> chatIds = aiServiceImpl.getChatIds(String.valueOf(userId));
        return Result.success(chatIds);
    }

    /**
     * 根据业务类型、chatId查询会话历史
     * @param userId 用户ID
     * @param type 业务类型
     * @param chatId 会话id
     * @return 指定会话的历史消息
     */
    @GetMapping("/history/{type}/{chatId}")
    @Operation(summary = "查询单个会话历史")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@RequestParam("userId") Long userId, @PathVariable("type") AiType type, @PathVariable("chatId") String chatId) {
        List<ChatMessageDTO> messages = aiServiceImpl.getChatHistory(String.valueOf(userId), chatId);
        return Result.success(messages);
    }

}
