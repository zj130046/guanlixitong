package com.aics.ticket.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Mock 向量模型 —— 基于文本哈希生成 64 维归一化伪向量（相同文本向量一致）
 */
public class MockEmbeddingModel implements EmbeddingModel {

    private static final int DIM = 64;

    @Override
    public float[] embed(Document document) {
        return embedText(document == null ? "" : document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<float[]> vectors = request.getInstructions().stream().map(this::embedText).toList();
        List<Embedding> embeddings = IntStream.range(0, vectors.size())
                .mapToObj(i -> new Embedding(vectors.get(i), i))
                .toList();
        return new EmbeddingResponse(embeddings);
    }

    private float[] embedText(String text) {
        int hash = text == null ? 0 : text.hashCode();
        List<Double> raw = new ArrayList<>(DIM);
        for (int i = 0; i < DIM; i++) {
            double val = Math.sin(hash + i * 0.1) * 0.5 + 0.5;
            raw.add(val);
        }
        double norm = Math.sqrt(raw.stream().mapToDouble(v -> v * v).sum());
        float[] vec = new float[DIM];
        for (int i = 0; i < DIM; i++) {
            vec[i] = (float) (raw.get(i) / norm);
        }
        return vec;
    }
}
