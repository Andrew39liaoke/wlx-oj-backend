package com.wlx.ojbackendaiservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.Serial;
import java.io.Serializable;

/**
 * MCP 工具接口
 * 提供文生图和联网搜索两个接口，分别调用阿里云百炼 MCP 服务
 */
@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RequestMapping("/ai/mcp")
@Tag(name = "MCP工具接口", description = "提供文生图和联网搜索功能")
public class McpToolsController {

    /**
     * 文生图 ChatClient，注入 MCP TextToImage 工具
     */
    @Resource(name = "textToImageChatClient")
    private ChatClient textToImageChatClient;

    /**
     * 联网搜索 ChatClient，注入 MCP WebSearch 工具
     */
    @Resource(name = "webSearchChatClient")
    private ChatClient webSearchChatClient;

    // ==================== 文生图接口 ====================

    /**
     * 文生图（流式 SSE）
     *
     * @param request 请求体，prompt 填写图片描述
     * @return 流式返回包含图片 URL 的文本
     */
    @PostMapping(value = "/text-to-image", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文生图（流式）", description = "调用阿里云百炼万相-文生图 MCP 服务，根据文字描述生成图片并返回图片 URL")
    public Flux<ServerSentEvent<Object>> textToImage(@RequestBody @Valid McpRequest request) {
        return textToImageChatClient.prompt()
                .user(request.getPrompt())
                .stream()
                .content()
                .map(content -> ServerSentEvent.<Object>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<Object>builder()
                                .data("[complete]")
                                .build()
                ));
    }

    // ==================== 联网搜索接口 ====================

    /**
     * 联网搜索（流式 SSE）
     *
     * @param request 请求体，prompt 填写搜索问题
     * @return 流式返回搜索结果内容
     */
    @PostMapping(value = "/web-search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "联网搜索（流式）", description = "调用阿里云百炼联网搜索 MCP 服务，实时获取互联网信息并综合回答")
    public Flux<ServerSentEvent<Object>> webSearch(@RequestBody @Valid McpRequest request) {
        return webSearchChatClient.prompt()
                .user(request.getPrompt())
                .stream()
                .content()
                .map(content -> ServerSentEvent.<Object>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<Object>builder()
                                .data("[complete]")
                                .build()
                ));
    }

    // ==================== 请求体 ====================

    /**
     * MCP 工具通用请求体
     */
    @Data
    public static class McpRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "请求内容不能为空")
        private String prompt;
    }
}
