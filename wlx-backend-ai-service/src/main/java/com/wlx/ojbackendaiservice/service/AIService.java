package com.wlx.ojbackendaiservice.service;

import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendmodel.model.dto.ai.AiInputDTO;
import jakarta.validation.Valid;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * AI服务
 */
public interface AIService {
    /**
     * 获取聊天
     *
     * @param aiInputDTO Ai输入
     * @return Flux < result< string>>
     */
    Flux<ServerSentEvent<Object>> getChat(AiInputDTO aiInputDTO);

    /**
     * 生成题目
     *
     * @param require 题目要求
     * @return Flux < result< string>>
     */
    Flux<ServerSentEvent<Object>> generateQuestion(String require);

    /**
     * questionAnswer方法，处理AI输入数据并返回结果流
     *
     * @param aiInputDTO 有效验证的AI输入数据传输对象
     * @return 包含字符串结果的Flux流
     */
    Flux<ServerSentEvent<Object>> questionAnswer(@Valid AiInputDTO aiInputDTO);
}
