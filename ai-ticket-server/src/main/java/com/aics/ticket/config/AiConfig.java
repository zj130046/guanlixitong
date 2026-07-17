package com.aics.ticket.config;

import com.aics.ticket.ai.AiChatClient;
import com.aics.ticket.ai.DeepSeekAiChatClient;
import com.aics.ticket.ai.MockAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * AI 客户端配置
 * 根据 app.ai.provider 配置决定使用 DeepSeek 还是 Mock
 */
@Configuration
public class AiConfig {

    @Value("${app.ai.provider:mock}")
    private String provider;

    @Value("${app.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${app.ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${app.ai.deepseek.chat-model:deepseek-chat}")
    private String chatModel;

    @Value("${app.ai.deepseek.embed-model:text-embedding-v2}")
    private String embedModel;

    @Bean
    @Primary
    public AiChatClient aiChatClient() {
        if ("deepseek".equalsIgnoreCase(provider) && StringUtils.hasText(apiKey)) {
            return new DeepSeekAiChatClient(apiKey, baseUrl, chatModel, embedModel);
        }
        return new MockAiChatClient();
    }
}
