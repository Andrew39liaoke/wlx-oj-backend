package com.wlx.ojbackendaiservice.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import com.wlx.ojbackendaiservice.tools.KnowledgeTools;
import com.wlx.ojbackendaiservice.tools.ProblemRecommendationTools;
import com.wlx.ojbackendaiservice.tools.ProblemSolutionTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaaLLMConfig
{
    @Value("${spring.ai.dashscope.api-key:}")
    private String apikey;

    @Resource
    private ProblemRecommendationTools problemRecommendationTools;

    @Resource
    private ProblemSolutionTools problemSolutionTools;

    @Resource
    private KnowledgeTools knowledgeTools;


    @Bean("chatModel")
    public ChatModel chatModel()
    {
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder()
                        .apiKey(apikey)
                        .build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .model("qwen3-max")
                                .build()
                )
                .build();
    }



    /**
     * 创建聊天客户端的方法
     *
     * @param chatModel      聊天模型
     */
    @Bean("chatClient")
    public ChatClient chatClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository){
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem(AiSystemConstant.DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .defaultTools(problemRecommendationTools)
                .build();
    }

    @Bean("titleChatClient")
    public ChatClient titleChatClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository){
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(5)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem(AiSystemConstant.TITLE_GENERATION)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .defaultTools(problemRecommendationTools)
                .build();
    }

    @Bean("questionAnswerClient")
    public ChatClient questionAnswerClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository){
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem(AiSystemConstant.CODE_SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .defaultTools(problemSolutionTools)
                .build();
    }
    /**
     * 创建生成问题的聊天客户端
     *
     * @param chatModel 聊天模型实例
     */
    @Bean("generateQuestionClient")
    public ChatClient generateQuestionClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository){
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(5)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem(AiSystemConstant.GENERATE_QUESTION)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .defaultTools(problemSolutionTools)
                .build();
    }

    @Bean("autoFillQuestionClient")
    public ChatClient autoFillQuestionClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository) {
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(5)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem(AiSystemConstant.QUESTION_AUTO_FILL_SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .defaultTools(knowledgeTools)
                .build();
    }

    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(ChatModel chatModel, JedisRedisChatMemoryRepository redisChatMemoryRepository)
    {
        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10)
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(windowChatMemory).build())
                .build();
    }

    /**
     * 面试鸭 MCP 聊天客户端
     * 注入 MCP ToolCallbackProvider，自动绑定面试鸭 questionSearch 工具
     *
     * @param chatModel 聊天模型
     * @param mcpTools  MCP 工具集（由 spring-ai-starter-mcp-client 自动装配）
     */
/*    @Bean("mianshiyaChatClient")
    public ChatClient mianshiyaChatClient(ChatModel chatModel, ToolCallbackProvider mcpTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(AiSystemConstant.MIANSHIYA_SEARCH_PROMPT)
                .defaultTools(mcpTools)
                .build();
    }*/

    /**
     * 文生图 MCP 聊天客户端
     * 调用阿里云百炼 万相-文生图 MCP 服务，根据描述生成图片并返回 URL
     *
     * @param chatModel 聊天模型
     * @param mcpTools  MCP 工具集（由 McpClientConfig 手动配置）
     */
    @Bean("textToImageChatClient") // 你可以根据需要决定是否重命名该 Bean
    public ChatClient textToImageChatClient(ChatModel chatModel, ToolCallbackProvider mcpTools) {
        return ChatClient.builder(chatModel)
                // 【核心修改点】：全新的系统提示词
                .defaultSystem("你是一个全能的多媒体生成助手，拥有强大的图片生成和视频生成能力。\n" +
                        "1. **文生图**：当用户想要一张图片时，使用图像生成工具。工具会返回图片链接。你直接向用户展示图片链接（Markdown格式）。\n" +
                        "2. **文生视频**：由于视频生成（万相2.5）是耗时的排队任务。调用相关工具后，" +
                        "如果工具返回了`task_id`（任务ID）和`PENDING`（排队中）状态，" +
                        "你**必须**把该 `task_id` 清楚地打印给用户，并告知用户任务已成功提交，可以使用查询工具后续查询结果。" +
                        "**严禁**隐瞒`task_id`，**严禁**只回答‘请稍等片刻’，严禁欺骗用户生成已完成。")
                .defaultToolCallbacks(mcpTools)
                // 推荐加上这个 Advisor，在 IDEA 控制台能看到详细的工具调用 JSON
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 联网搜索 MCP 聊天客户端
     * 调用阿里云百炼 联网搜索 MCP 服务，获取实时互联网信息
     */
    @Bean("webSearchChatClient")
    public ChatClient webSearchChatClient(ChatModel chatModel, ToolCallbackProvider mcpTools) {
        return ChatClient.builder(chatModel)
                // 【核心修改点】：在提示词中明确要求输出来源链接，并规定 Markdown 格式
                .defaultSystem("你是一个专业、严谨的智能搜索助手。当用户提出问题（尤其是查询新闻、资讯或事实）时，请务必遵守以下规则：\n" +
                        "1. 优先调用联网搜索工具获取实时最新信息。\n" +
                        "2. 结合搜索结果进行准确、全面的回答。\n" +
                        "3. **必须给出信息来源**：在引用新闻或关键事实后，必须使用 Markdown 格式附带原始的真实来源链接，格式为：[网页/新闻标题](具体的URL链接)。\n" +
                        "4. **严禁伪造链接**：只能使用搜索工具实际返回的真实 URL，绝不能自己编造域名或链接。")
                .defaultToolCallbacks(mcpTools)
                .defaultAdvisors(new SimpleLoggerAdvisor()) // 加上这个方便你在控制台看它到底有没有拿到链接
                .build();
    }
}