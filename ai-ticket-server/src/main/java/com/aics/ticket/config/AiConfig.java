package com.aics.ticket.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.aics.ticket.ai.MockChatModel;
import com.aics.ticket.ai.MockEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * AI 模型装配 —— 按 app.ai.provider / app.ai.embedding-provider 决定真实模型或 Mock 兜底。
 * <p>
 * 对话（ChatModel）与向量（EmbeddingModel）完全解耦：DeepSeek 无 embedding API，
 * 推荐组合为 provider=deepseek + embedding-provider=dashscope。
 * 任一 provider 缺少 API Key 时自动回退 Mock，保证无 Key 也能启动。
 * 扩展新 provider：加一个依赖 + 一个分支 + 一段配置即可。
 * </p>
 */
@Configuration
public class AiConfig {

    @Value("${app.ai.provider:mock}")
    private String provider;

    @Value("${app.ai.embedding-provider:mock}")
    private String embeddingProvider;

    @Value("${app.ai.deepseek.api-key:}")
    private String dsApiKey;

    @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}")
    private String dsBaseUrl;

    @Value("${app.ai.deepseek.completions-path:/v1/chat/completions}")
    private String dsCompletionsPath;

    @Value("${app.ai.deepseek.chat-model:deepseek-chat}")
    private String dsChatModel;

    @Value("${app.ai.dashscope.api-key:}")
    private String dscApiKey;

    @Value("${app.ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String dscBaseUrl;

    @Value("${app.ai.dashscope.chat-model:qwen-plus}")
    private String dscChatModel;

    @Bean
    @Primary
    public ChatModel chatModel() {
        // DeepSeek：OpenAI 兼容 API（base-url 只写 host，completions-path 单独配，避免 /v1/v1）
        if ("deepseek".equalsIgnoreCase(provider) && StringUtils.hasText(dsApiKey)) {
            return OpenAiChatModel.builder()
                    .openAiApi(OpenAiApi.builder()
                            .apiKey(dsApiKey)
                            .baseUrl(dsBaseUrl)
                            .completionsPath(dsCompletionsPath)
                            .build())
                    .defaultOptions(OpenAiChatOptions.builder().model(dsChatModel).build())
                    .build();
        }
        // DashScope（阿里云百炼）
        if ("dashscope".equalsIgnoreCase(provider) && StringUtils.hasText(dscApiKey)) {
            return DashScopeChatModel.builder()
                    .dashScopeApi(DashScopeApi.builder()
                            .apiKey(dscApiKey)
                            .baseUrl(dscBaseUrl)
                            .build())
                    .defaultOptions(DashScopeChatOptions.builder().withModel(dscChatModel).build())
                    .build();
        }
        return new MockChatModel();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // 向量 provider：默认 text-embedding-v2（new DashScopeEmbeddingModel(api) 的默认模型）
        if ("dashscope".equalsIgnoreCase(embeddingProvider) && StringUtils.hasText(dscApiKey)) {
            return new DashScopeEmbeddingModel(DashScopeApi.builder()
                    .apiKey(dscApiKey)
                    .baseUrl(dscBaseUrl)
                    .build());
        }
        return new MockEmbeddingModel();
    }
}
