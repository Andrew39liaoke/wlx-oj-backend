package com.wlx.ojbackendaiservice.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wlx.ojbackendmodel.model.dto.ai.ChatMessageDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 智能代码评估Agent服务
 */
@Slf4j
@Service
public class CodeEvaluationAgentService {

    @Resource
    private DashScopeChatModel chatModel;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private VectorSearchService vectorSearchService;
    
    /**
     * 获取用户的聊天历史记录
     */
    public List<ChatMessageDTO> getChatHistory(String userId) {
        try {
            // 使用 StringRedisTemplate 获取聊天历史，key 格式为 spring_ai_alibaba_chat_memory:{userId}
            String key = "spring_ai_alibaba_chat_memory:" + userId;
            // 使用 StringRedisTemplate 直接获取字符串类型的列表数据
            Long size = stringRedisTemplate.opsForList().size(key);

            if (size == null || size == 0) {
                return Collections.emptyList();
            }

            // 获取消息内容
            List<String> rawMessages;
            if (size > 10) {
                // 如果消息数量大于10条，获取最后10条
                rawMessages = stringRedisTemplate.opsForList().range(key, size - 10, size);
            } else {
                // 如果消息数量小于等于10条，获取所有
                rawMessages = stringRedisTemplate.opsForList().range(key, 0, -1);
            }

            if (rawMessages == null || rawMessages.isEmpty()) {
                return Collections.emptyList();
            }
            // 转换为DTO列表
            List<ChatMessageDTO> messages = new ArrayList<>();
            for (int i = 0; i < rawMessages.size(); i++) {
                String msgStr = rawMessages.get(i);
                try {
                    // 使用 ObjectMapper 解析 JSON 字符串为 Map
                    Map<?, ?> msgMap = objectMapper.readValue(msgStr, new TypeReference<Map<?, ?>>() {
                    });
                    ChatMessageDTO dto = new ChatMessageDTO();
                    dto.setMessageType((String) msgMap.get("messageType"));
                    dto.setTextContent((String) msgMap.get("textContent"));
                    dto.setMetadata((Map<String, Object>) msgMap.get("metadata"));
                    dto.setMedia((List<Object>) msgMap.get("media"));
                    messages.add(dto);

                    String preview = dto.getTextContent() != null && dto.getTextContent().length() > 100
                            ? dto.getTextContent().substring(0, 100) + "..."
                            : dto.getTextContent();
                    log.debug("消息[{}]内容预览: {}", i, preview);
                } catch (Exception e) {
                    log.warn("解析消息[{}]失败: {}", i, e.getMessage());
                }
            }

            log.info("用户 {} 的聊天历史转换完成，共 {} 条消息", userId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("获取聊天历史失败，用户ID: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 流式评估用户提交的代码
     */
    public Flux<String> evaluateCodeStream(String message, String userId) {
        try {
            // 使用ChatClient进行流式调用
            String systemPrompt = """
                    你是一个智能判题系统，负责评估用户提交的代码，并根据题目内容、判题配置和测试用例进行全面分析。

                    ## 核心职责

                    ### 1. 解析题目内容和配置
                    - 阅读题目描述和判题配置
                    - 理解题目要求
                    - 确保代码评估符合题目要求

                    ### 2. 执行测试用例
                    - 根据题目配置的测试用例对用户提交的代码进行自动化执行
                    - 评估代码在各个测试用例下的表现
                    - 只有通过所有测试用例的代码才算是通过该题目

                    ### 3. 判定代码正确性
                    - 根据测试用例的结果，判断代码是否正确
                    - 如果有任何测试用例未通过，标记为错误并给出具体错误信息

                    ### 4. 错误反馈与定位
                    - 如果代码错误，输出具体的错误行、错误类型和发生错误的测试用例编号
                    - 帮助用户快速定位问题所在

                    ### 5. 优化建议
                    - 如果代码能通过测试用例，但存在优化空间，需给出相应的优化建议
                    - 分析维度包括：
                      - 时间复杂度
                      - 空间复杂度
                      - 算法效率
                      - 数据结构合理性

                    ### 6. 多编程语言支持
                    - 支持多种编程语言的代码分析（如 Python、Java、C++ 等）
                    - 根据语言特性进行相应的评估和分析

                    ### 7. 评估标准
                    - 确保评估标准基于题目的具体要求
                    - 评估维度包括：
                      - 正确性
                      - 代码效率
                      - 代码可读性
                      - 代码可维护性

                    ## 输出规范

                    ### 代码通过所有测试用例
                    "恭喜！您的代码已通过所有测试用例，题目通过。"

                    ### 代码未通过某个测试用例
                    "您的代码未通过测试用例 [用例编号]。错误类型：[错误类型]。发生错误的位置在第 [行号] 行，请检查[相关逻辑]。"

                    ### 代码通过测试但有优化空间
                    "您的代码通过了所有测试用例，但建议优化[优化方向]。当前[现状描述]，建议[优化建议]。"

                    ## 注意事项

                    - 反馈信息应清晰、准确、易于理解
                    - 错误定位应精确到具体行号和错误类型
                    - 优化建议应基于算法原理和最佳实践
                    - 保持评估标准的一致性和公正性
                    """;
            return qwenChatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(message)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, userId))
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("流式代码评估失败", e);
            return Flux.just("评估失败: " + e.getMessage());
        }
    }

    /**
     * 使用向量数据库查询相关文档后进行流式代码评估
     * @param message 用户提交的消息（包含代码和问题）
     * @param userId 用户ID
     * @param topK 查询返回的文档数量，默认为5
     * @param similarityThreshold 相似度阈值，默认为0.7
     * @return 流式响应
     */
    public Flux<String> evaluateCodeWithVectorSearch(String message, String userId, Integer topK, Double similarityThreshold) {
        try {
            // 设置默认值
            int k = (topK != null && topK > 0) ? topK : 5;
            double threshold = (similarityThreshold != null && similarityThreshold > 0) ? similarityThreshold : 0.7;

            log.info("开始向量搜索，用户ID: {}, topK: {}, threshold: {}", userId, k, threshold);

            // 1. 使用向量数据库查询相关文档
            List<Document> relevantDocs = vectorSearchService.searchWithThreshold(message, k, threshold);

            // 2. 构建增强的上下文信息
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("## 相关知识库内容\n\n");

            if (relevantDocs.isEmpty()) {
                contextBuilder.append("未找到相关的知识库内容。\n\n");
                log.warn("向量搜索未找到相关文档");
            } else {
                log.info("找到 {} 个相关文档", relevantDocs.size());
                for (int i = 0; i < relevantDocs.size(); i++) {
                    Document doc = relevantDocs.get(i);
                    contextBuilder.append("### 参考资料 ").append(i + 1).append("\n");
                    System.out.println("doc.getText() = " + doc.getText());
                    System.out.println("doc.getFormattedContent() = " + doc.getFormattedContent());
                    contextBuilder.append(doc.getText()).append("\n\n");

                    // 如果文档有元数据，也可以添加
                    if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
                        contextBuilder.append("来源: ").append(doc.getMetadata()).append("\n\n");
                    }
                }
            }

            // 3. 构建完整的用户消息
            String enhancedMessage = contextBuilder.toString() + "## 用户问题\n\n" + message;

            // 4. 使用ChatClient进行流式调用
            String systemPrompt = """
                    你是一个智能判题系统，根据用户的问题，给出相应的答案。
                    """;

            log.info("开始流式回答，用户ID: {}", userId);

            return qwenChatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(enhancedMessage)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, userId))
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("向量搜索流式代码评估失败，用户ID: {}", userId, e);
            return Flux.just("评估失败: " + e.getMessage());
        }
    }




    /**
     * 调用AI模型
     */
    private String callAIModel(String prompt) {
        try {
            // 使用DashScope聊天模型
            String response = chatModel.call(prompt);
            return response;
        } catch (Exception e) {
            log.error("调用AI模型失败", e);
            throw new RuntimeException("AI模型调用失败: " + e.getMessage());
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJson(String response) {
        // 查找JSON代码块
        int jsonStart = response.indexOf("```json");
        int jsonEnd = response.lastIndexOf("```");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            // 提取JSON代码块内容
            String json = response.substring(jsonStart + 7, jsonEnd).trim();
            return json;
        }

        // 如果没有代码块标记，尝试查找JSON对象
        int braceStart = response.indexOf("{");
        int braceEnd = response.lastIndexOf("}");

        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return response.substring(braceStart, braceEnd + 1).trim();
        }

        throw new RuntimeException("无法从AI响应中提取JSON");
    }

}
