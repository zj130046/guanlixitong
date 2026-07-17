package com.aics.ticket.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * 向量工具类 —— 余弦相似度计算 + JSON 序列化
 */
public class VectorUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.size() != b.size() || a.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static String toJson(List<Double> vector) {
        try {
            return MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Double> fromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
