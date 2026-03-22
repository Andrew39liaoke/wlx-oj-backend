package com.wlx.ojbackendaiservice.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * MCP 客户端配置类
 * 手动创建 WebSearch（Streamable HTTP）和 TextToImage（SSE）MCP 客户端
 * 使用自定义 Authorization 请求头连接阿里云百炼 MCP 服务
 */
@Configuration
public class McpClientConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 联网搜索 MCP 客户端（Streamable HTTP 传输）
     * 连接阿里云百炼联网搜索 MCP 服务
     */
    @Bean(name = "webSearchMcpClient", destroyMethod = "close")
    public McpSyncClient webSearchMcpClient() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                // 【修改点 1】：baseUrl 只保留到根域名
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey);

        WebClientStreamableHttpTransport transport = WebClientStreamableHttpTransport
                .builder(webClientBuilder)
                .endpoint("/api/v1/mcps/WebSearch/mcp")
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(120))
                .build();
        client.initialize();
        return client;
    }

    /**
     * 文生图 MCP 客户端（SSE 传输）
     * 连接阿里云百炼万相-文生图 MCP 服务
     */
/*    @Bean(name = "textToImageMcpClient", destroyMethod = "close")
    public McpSyncClient textToImageMcpClient() {
        WebClient.Builder webClientBuilder = WebClient.builder()
                // 【修改点 1】：baseUrl 只保留到根域名
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey);

        WebFluxSseClientTransport transport = new WebFluxSseClientTransport(
                webClientBuilder,
                McpJsonMapper.createDefault(),
                // 【修改点 2】：把完整的请求路径移到这里
                "/api/v1/mcps/TextToImage/sse"
        );

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(120))
                .build();
        client.initialize();
        return client;
    }*/
    @Bean(name = "wan25MediaMcpClient", destroyMethod = "close") // 建议改个名，体现它不仅能生图还能生视频
    public McpSyncClient wan25MediaMcpClient() {

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey);
        WebClientStreamableHttpTransport transport = WebClientStreamableHttpTransport
                .builder(webClientBuilder)
                // 把完整的相对路径放在 endpoint 这里
                .endpoint("/api/v1/mcps/Wan25Media/mcp")
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(300))
                .build();
        client.initialize();

        return client;
    }

    /**
     * MCP 工具回调提供者
     * 整合 WebSearch 和 TextToImage 两个 MCP 客户端的工具，供 ChatClient 使用
     */
    @Bean("mcpTools")
    public ToolCallbackProvider mcpToolCallbackProvider(McpSyncClient webSearchMcpClient,
                                                       McpSyncClient wan25MediaMcpClient) {
        return new SyncMcpToolCallbackProvider(List.of(webSearchMcpClient, wan25MediaMcpClient));
    }
}
