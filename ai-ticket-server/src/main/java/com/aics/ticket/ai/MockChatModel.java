package com.aics.ticket.ai;

import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Mock 对话模型 —— 无 API Key 或 provider=mock 时使用，返回模板化回复/分块
 */
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String msg = extractLastUserMessage(prompt);
        String text = "已收到您的问题：\"" + msg + "\"。我会先从 FAQ 和知识库中检索答案，若无法解决将自动生成工单转人工处理。\n\n" +
                "（当前为演示模式，接入真实大模型后可获得更智能的回复）";
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String msg = extractLastUserMessage(prompt);
        List<String> chunks = List.of(
                "已收到您的问题：", msg, "。",
                "\n\n正在检索 FAQ 知识库...",
                "\n已为您匹配相关答案。",
                "\n\n如仍无法解决，您可以点击「转人工」生成工单，客服会尽快处理。"
        );
        return Flux.fromIterable(chunks)
                .map(c -> new ChatResponse(List.of(new Generation(new AssistantMessage(c)))))
                .delaySequence(Duration.ofMillis(80));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    private String extractLastUserMessage(Prompt prompt) {
        if (prompt == null || prompt.getInstructions() == null) return "";
        return prompt.getInstructions().stream()
                .filter(m -> m instanceof UserMessage)
                .reduce((first, second) -> second)
                .map(Message::getText)
                .orElse("");
    }
}
