package com.aics.ticket.ai;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Mock AI 客户端 —— 无 API Key 时使用，返回模板化回复
 */
@Service
public class MockAiChatClient implements AiChatClient {

    @Override
    public String complete(String message) {
        return "已收到您的问题：\"" + message + "\"。我会先从 FAQ 和知识库中检索答案，若无法解决将自动生成工单转人工处理。\n\n" +
               "（当前为演示模式，接入真实大模型后可获得更智能的回复）";
    }

    @Override
    public List<String> stream(String message) {
        return List.of(
            "已收到您的问题：", message, "。",
            "\n\n正在检索 FAQ 知识库...",
            "\n已为您匹配相关答案。",
            "\n\n如仍无法解决，您可以点击「转人工」生成工单，客服会尽快处理。"
        );
    }

    @Override
    public List<Double> embed(String text) {
        // Mock: 返回伪向量（基于文本哈希生成，保证相同文本向量一致）
        int dim = 64;
        List<Double> vec = new ArrayList<>(dim);
        int hash = text == null ? 0 : text.hashCode();
        for (int i = 0; i < dim; i++) {
            double val = Math.sin(hash + i * 0.1) * 0.5 + 0.5;
            vec.add(val);
        }
        double norm = Math.sqrt(vec.stream().mapToDouble(v -> v * v).sum());
        return vec.stream().map(v -> v / norm).toList();
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
