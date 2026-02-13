package com.wlx.ojbackendaiservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wlx.ojbackendaiservice.config.AiSystemConstant;
import com.wlx.ojbackendaiservice.service.AIService;
import com.wlx.ojbackendcommon.common.MessageConstant;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.AiException;
import com.wlx.ojbackendmodel.model.dto.ai.AiInputDTO;
import com.wlx.ojbackendmodel.model.dto.ai.ChatMessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
@Slf4j
public class AIServiceImpl implements AIService {
    @Resource
    private ChatClient chatClient ;

    @Resource
    private ChatClient titleChatClient;

    @Resource
    private ChatClient generateQuestionClient;

    @Resource
    private ChatClient questionAnswerClient;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取对话历史
     * @param userId 用户ID
     * @param chatId 对话ID
     * @return 聊天消息列表
     */
    public List<ChatMessageDTO> getChatHistory(String userId, String chatId) {
        try {
            // 键格式: spring_ai_alibaba_chat_memory:{userId}:{chatId}
            String key = "spring_ai_alibaba_chat_memory:" + userId + ":" + chatId;
            Long size = stringRedisTemplate.opsForList().size(key);

            if (size == null || size == 0) {
                return Collections.emptyList();
            }

            // 获取最后10条消息（与 CodeEvaluationAgentService 保持一致）
            List<String> rawMessages;
            if (size > 10) {
                rawMessages = stringRedisTemplate.opsForList().range(key, size - 10, size);
            } else {
                rawMessages = stringRedisTemplate.opsForList().range(key, 0, -1);
            }

            if (rawMessages == null || rawMessages.isEmpty()) {
                return Collections.emptyList();
            }

            // 转换为 DTO 列表
            List<ChatMessageDTO> messages = new ArrayList<>();
            for (String msgStr : rawMessages) {
                try {
                    Map<?, ?> msgMap = objectMapper.readValue(msgStr, new TypeReference<>() {
                    });
                    ChatMessageDTO dto = new ChatMessageDTO();
                    dto.setMessageType((String) msgMap.get("messageType"));
                    dto.setTextContent((String) msgMap.get("textContent"));
                    dto.setMetadata((Map<String, Object>) msgMap.get("metadata"));
                    dto.setMedia((List<Object>) msgMap.get("media"));
                    messages.add(dto);
                } catch (Exception e) {
                    log.warn("解析消息失败: {}", e.getMessage());
                }
            }

            log.info("用户 {} 对话 {} 的聊天历史转换完成，共 {} 条消息", userId, chatId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("获取聊天历史失败，用户ID: {}, 对话ID: {}", userId, chatId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户所有对话的历史记录
     * @param userId 用户ID
     * @return 所有对话的消息列表
     */
    public List<ChatMessageDTO> getAllChatHistoryByUserId(String userId) {
        try {
            // 键格式: spring_ai_alibaba_chat_memory:{userId}:{chatId}
            // 获取用户相关的所有 key
            String pattern = "spring_ai_alibaba_chat_memory:" + userId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }

            // 收集所有消息
            List<ChatMessageDTO> allMessages = new ArrayList<>();
            for (String key : keys) {
                Long size = stringRedisTemplate.opsForList().size(key);
                if (size == null || size == 0) {
                    continue;
                }
                List<String> rawMessages = stringRedisTemplate.opsForList().range(key, Math.max(0, size - 5), size);
                if (rawMessages != null) {
                    for (String msgStr : rawMessages) {
                        try {
                            Map<?, ?> msgMap = objectMapper.readValue(msgStr, new TypeReference<>() {
                            });
                            ChatMessageDTO dto = new ChatMessageDTO();
                            dto.setMessageType((String) msgMap.get("messageType"));
                            dto.setTextContent((String) msgMap.get("textContent"));
                            dto.setMetadata((Map<String, Object>) msgMap.get("metadata"));
                            dto.setMedia((List<Object>) msgMap.get("media"));
                            allMessages.add(dto);
                        } catch (Exception e) {
                            log.warn("解析消息失败: {}", e.getMessage());
                        }
                    }
                }
            }

            log.info("用户 {} 的所有聊天历史转换完成，共 {} 条消息", userId, allMessages.size());
            return allMessages;

        } catch (Exception e) {
            log.error("获取用户所有聊天历史失败，用户ID: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取聊天内容的方法
     * @param aiInputDTO 输入数据传输对象，包含用户ID、聊天ID和提示信息
     * @return 返回包含聊天结果的数据流
     */
    @Override
    public Flux<ServerSentEvent<Object>> getChat(AiInputDTO aiInputDTO) {
        String userId = String.valueOf(aiInputDTO.getUserId());
        String chatId = aiInputDTO.getChatId();

        // 使用 StringRedisTemplate 获取对话历史
        List<ChatMessageDTO> chatHistory = getChatHistory(userId, chatId);

        String systemPrompt;
        switch (aiInputDTO.getType()) {
            case CHAT -> systemPrompt = AiSystemConstant.getSmartRecommendations();
            case CODE -> systemPrompt = AiSystemConstant.CODE_SYSTEM_PROMPT;
            default -> throw new AiException(MessageConstant.AI_CHAT_TYPE_NOT_FOUND);
        }

        // 使用 CONVERSATION_ID 参数，格式为 userId:chatId（实现 userId + chatId 组合存储）
        String conversationKey = userId + ":" + chatId;
        Flux<ServerSentEvent<Object>> contentStream = chatClient.prompt()
                .user(aiInputDTO.getPrompt())
                .system(systemPrompt)
                .advisors()
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationKey))
                .stream()
                .content()
                .map(content -> ServerSentEvent.builder()
                        .data(Result.success(content))
                        .build()
                );

        Mono<ServerSentEvent<Object>> titleMono;
        if (chatHistory.size() <= 4) {
            titleMono = Mono.fromCallable(() -> {
                    String title = titleChatClient.prompt()
                            .user(aiInputDTO.getPrompt())
                            .call()
                            .content();
                    log.info("生成标题: {}", title);
                    return Result.success(title);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(t -> ServerSentEvent.builder()
                        .event("title")
                        .data(t)
                        .build()
                );
        } else {
            titleMono = Mono.empty();
        }

        return Flux.merge(contentStream, titleMono);
    }

    /**
     * 题目答疑方法
     * @param aiInputDTO 输入数据
     * @return 流式响应
     */
    @Override
    public Flux<ServerSentEvent<Object>> questionAnswer(AiInputDTO aiInputDTO) {
        try {
            log.info("【questionAnswer】开始处理，入参: userId={}, chatId={}, prompt={}, type={}, problemId={}",
                    aiInputDTO.getUserId(), aiInputDTO.getChatId(), aiInputDTO.getPrompt(),
                    aiInputDTO.getType(), aiInputDTO.getProblemId());

            String userId = String.valueOf(aiInputDTO.getUserId());
            String chatId = aiInputDTO.getChatId();

            // 使用 CONVERSATION_ID 参数，格式为 userId:chatId（实现 userId + chatId 组合存储）
            String conversationKey = userId + ":" + chatId;

            // 始终使用 CODE_SYSTEM_PROMPT + problemId，让 AI 能调用工具获取题目解答
            String systemPrompt = AiSystemConstant.CODE_SYSTEM_PROMPT + aiInputDTO.getProblemId();

            // 尝试从向量库检索补充上下文
            VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever
                    .builder()
                    .vectorStore(vectorStore)
                    .topK(1)
                    .similarityThreshold(0.8)
                    .build();

            List<Document> documents = documentRetriever.retrieve(new Query(aiInputDTO.getPrompt()));
            log.info("【questionAnswer】向量库检索完成，找到 {} 个文档", documents.size());

            // 构建 ChatClient 请求
            var promptSpec = questionAnswerClient.prompt()
                    .user(aiInputDTO.getPrompt())
                    .system(systemPrompt)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationKey));

            // 如果向量库有匹配文档，附加 RAG advisor 作为补充上下文
            if (!documents.isEmpty()) {
                log.info("【questionAnswer】附加知识库 RAG 上下文，共 {} 个文档", documents.size());
                RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(documentRetriever)
                        .build();
                promptSpec = promptSpec.advisors(retrievalAugmentationAdvisor);
            }

            log.info("【questionAnswer】开始调用AI模型...");
            return promptSpec
                    .stream()
                    .content()
                    .map(content -> ServerSentEvent.builder()
                            .data(Result.success(content))
                            .build()
                    );
        } catch (Exception e) {
            log.error("【questionAnswer】处理异常，错误信息: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取用户所有对话的chatId列表
     * @param userId 用户ID
     * @return chatId列表
     */
    public List<String> getChatIds(String userId) {
        try {
            // 键格式: spring_ai_alibaba_chat_memory:{userId}:{chatId}
            String pattern = "spring_ai_alibaba_chat_memory:" + userId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }

            // 提取 chatId（key 的最后部分）
            return keys.stream()
                    .map(key -> {
                        String[] parts = key.split(":");
                        return parts.length > 0 ? parts[parts.length - 1] : null;
                    })
                    .filter(chatId -> chatId != null && !chatId.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户对话ID列表失败，用户ID: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成题目的方法
     * @param require 题目要求
     * @return 返回包含生成题目的数据流
     */
    @Override
    public Flux<ServerSentEvent<Object>> generateQuestion(String require) {
        return generateQuestionClient.prompt()
                .user(require)
                .stream()
                .content()
                .map(content -> ServerSentEvent.builder()
                        .data(Result.success(content))
                        .build()
                );
    }

}
