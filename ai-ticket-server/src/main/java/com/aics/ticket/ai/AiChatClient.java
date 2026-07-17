package com.aics.ticket.ai;

import java.util.List;

/**
 * AI 对话客户端接口
 */
public interface AiChatClient {

    /**
     * 完整对话（一次性返回）
     */
    String complete(String message);

    /**
     * 流式对话（返回分块列表，Mock 用；真实流式走 SSE）
     */
    List<String> stream(String message);

    /**
     * 生成文本的向量 embedding
     */
    List<Double> embed(String text);

    /**
     * 批量生成向量
     */
    List<List<Double>> embedBatch(List<String> texts);
}
