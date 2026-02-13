package com.wlx.ojbackendaiservice.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import com.wlx.ojbackendaiservice.tools.ProblemRecommendationTools;
import com.wlx.ojbackendaiservice.tools.ProblemSolutionTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
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
}