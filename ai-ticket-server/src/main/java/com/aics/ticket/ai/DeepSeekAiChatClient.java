package com.aics.ticket.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * DeepSeek AI 客户端
 * 直连 DeepSeek API，支持 chat completions 和 embeddings
 */
public class DeepSeekAiChatClient implements AiChatClient {

    private final String apiKey;
    private final String baseUrl;
    private final String chatModel;
    private final String embedModel;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekAiChatClient(String apiKey, String baseUrl, String chatModel, String embedModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.chatModel = chatModel;
        this.embedModel = embedModel;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // 配置 RestTemplate 超时，避免 API 响应慢导致无限阻塞
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 连接超时 10s
        factory.setReadTimeout(120_000);     // 读取超时 120s（流式需较长，配合 SSE 超时）
        this.restTemplate.setRequestFactory(factory);
    }

    @Override
    public String complete(String message) {
        try {
            String url = baseUrl + "/chat/completions";
            String body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
                put("model", chatModel);
                put("stream", false);
                put("messages", List.of(
                    new java.util.HashMap<String, String>() {{
                        put("role", "system");
                        put("content", "你是一个智能客服助手，需要礼貌、专业、简洁地回答用户问题。如果遇到无法解答的问题，请引导用户转人工客服。");
                    }},
                    new java.util.HashMap<String, String>() {{
                        put("role", "user");
                        put("content", message);
                    }}
                ));
            }});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek API 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> stream(String message) {
        // 非流式环境下返回完整句子列表（仅作兼容，真流式走 streamChat 方法）
        String full = complete(message);
        return List.of(full);
    }

    /**
     * 真正的流式对话，逐 token 回调
     */
    public void streamChat(String message, Consumer<String> onToken, Runnable onComplete) {
        try {
            String url = baseUrl + "/chat/completions";
            String body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
                put("model", chatModel);
                put("stream", true);
                put("messages", List.of(
                    new java.util.HashMap<String, String>() {{
                        put("role", "system");
                        put("content", "你是一个智能客服助手，需要礼貌、专业、简洁地回答用户问题。如果遇到无法解答的问题，请引导用户转人工客服。");
                    }},
                    new java.util.HashMap<String, String>() {{
                        put("role", "user");
                        put("content", message);
                    }}
                ));
            }});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "text/event-stream");

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            restTemplate.execute(url, org.springframework.http.HttpMethod.POST,
                req -> {
                    req.getHeaders().addAll(headers);
                    req.getBody().write(body.getBytes(StandardCharsets.UTF_8));
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    onComplete.run();
                                    return null;
                                }
                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    JsonNode delta = node.path("choices").get(0).path("delta");
                                    if (delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        if (content != null && !content.isEmpty()) {
                                            onToken.accept(content);
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // 跳过解析失败的行
                                }
                            }
                        }
                        onComplete.run();
                    }
                    return null;
                });
        } catch (Exception e) {
            onToken.accept("[AI 服务异常: " + e.getMessage() + "]");
            onComplete.run();
        }
    }

    @Override
    public List<Double> embed(String text) {
        try {
            String url = baseUrl + "/embeddings";
            String body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
                put("model", embedModel);
                put("input", text);
            }});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embedding = root.path("data").get(0).path("embedding");
            List<Double> vec = new ArrayList<>();
            for (JsonNode v : embedding) {
                vec.add(v.asDouble());
            }
            return vec;
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        // DeepSeek 支持批量 embedding，为简化起见逐个调用
        return texts.stream().map(this::embed).toList();
    }
}
