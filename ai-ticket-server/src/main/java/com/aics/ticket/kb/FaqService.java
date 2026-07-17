package com.aics.ticket.kb;

import com.aics.ticket.ai.AiChatClient;
import com.aics.ticket.common.BusinessException;
import com.aics.ticket.common.VectorUtils;
import com.aics.ticket.kb.mapper.FaqCategoryMapper;
import com.aics.ticket.kb.mapper.FaqEntryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FaqService {

    private final FaqCategoryMapper categoryMapper;
    private final FaqEntryMapper entryMapper;
    private final AiChatClient aiChatClient;

    @Value("${app.faq.similarity-threshold:0.72}")
    private double similarityThreshold;

    @Value("${app.faq.semantic-search-enabled:true}")
    private boolean semanticSearchEnabled;

    public FaqService(FaqCategoryMapper categoryMapper, FaqEntryMapper entryMapper, AiChatClient aiChatClient) {
        this.categoryMapper = categoryMapper;
        this.entryMapper = entryMapper;
        this.aiChatClient = aiChatClient;
    }

    // ========== 分类管理 ==========

    public List<FaqCategory> categories() {
        return categoryMapper.selectList(new QueryWrapper<FaqCategory>().orderByAsc("sort_order", "id"));
    }

    public FaqCategory createCategory(Map<String, Object> request) {
        FaqCategory category = new FaqCategory();
        category.name = required(request, "name");
        category.parentId = longValue(request.get("parentId"));
        category.sortOrder = intValue(request.get("sortOrder"), 0);
        categoryMapper.insert(category);
        return category;
    }

    public FaqCategory updateCategory(Long id, Map<String, Object> request) {
        FaqCategory category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(404, "FAQ 分类不存在");
        if (request.containsKey("name")) category.name = required(request, "name");
        if (request.containsKey("parentId")) category.parentId = longValue(request.get("parentId"));
        if (request.containsKey("sortOrder")) category.sortOrder = intValue(request.get("sortOrder"), 0);
        categoryMapper.updateById(category);
        return categoryMapper.selectById(id);
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    // ========== FAQ 问答管理 ==========

    public Page<FaqEntry> entriesPage(long page, long size, String keyword, Long categoryId, Boolean enabledOnly) {
        QueryWrapper<FaqEntry> wrapper = new QueryWrapper<FaqEntry>().orderByDesc("updated_at");
        if (Boolean.TRUE.equals(enabledOnly)) wrapper.eq("enabled", 1);
        if (categoryId != null) wrapper.eq("category_id", categoryId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like("question", keyword).or().like("answer", keyword).or().like("keywords", keyword));
        }
        return entryMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<FaqEntry> entries(String keyword, Long categoryId, Boolean enabledOnly) {
        QueryWrapper<FaqEntry> wrapper = new QueryWrapper<FaqEntry>().orderByDesc("updated_at");
        if (Boolean.TRUE.equals(enabledOnly)) wrapper.eq("enabled", 1);
        if (categoryId != null) wrapper.eq("category_id", categoryId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like("question", keyword).or().like("answer", keyword).or().like("keywords", keyword));
        }
        return entryMapper.selectList(wrapper);
    }

    public FaqEntry getEntry(Long id) {
        FaqEntry entry = entryMapper.selectById(id);
        if (entry == null) throw new BusinessException(404, "FAQ 不存在");
        return entry;
    }

    public FaqEntry createEntry(Map<String, Object> request) {
        FaqEntry entry = new FaqEntry();
        applyEntry(entry, request, true);
        // 生成向量
        generateVector(entry);
        entryMapper.insert(entry);
        return entry;
    }

    public FaqEntry updateEntry(Long id, Map<String, Object> request) {
        FaqEntry entry = entryMapper.selectById(id);
        if (entry == null) throw new BusinessException(404, "FAQ 不存在");
        applyEntry(entry, request, false);
        // 如果问题变了，重新生成向量
        if (request.containsKey("question")) {
            generateVector(entry);
        }
        entryMapper.updateById(entry);
        return entryMapper.selectById(id);
    }

    public void deleteEntry(Long id) {
        entryMapper.deleteById(id);
    }

    /**
     * 最佳匹配：先用语义检索（如果启用），再用关键词匹配兜底
     */
    public Optional<FaqMatchResult> bestMatchWithScore(String message) {
        if (!StringUtils.hasText(message)) return Optional.empty();

        // 1. 语义检索
        if (semanticSearchEnabled) {
            Optional<FaqMatchResult> semantic = semanticMatch(message);
            if (semantic.isPresent() && semantic.get().score >= similarityThreshold) {
                return semantic;
            }
        }

        // 2. 关键词精确匹配
        List<FaqEntry> keywordMatches = entries(message, null, true);
        if (!keywordMatches.isEmpty()) {
            return Optional.of(new FaqMatchResult(keywordMatches.get(0), 0.85, "keyword"));
        }

        // 3. 分词模糊匹配
        for (String token : message.split("[\\s,，。？?！!；;、]+")) {
            if (token.length() < 2) continue;
            List<FaqEntry> tokenMatches = entries(token, null, true);
            if (!tokenMatches.isEmpty()) {
                return Optional.of(new FaqMatchResult(tokenMatches.get(0), 0.65, "fuzzy"));
            }
        }

        return Optional.empty();
    }

    /**
     * 兼容旧接口
     */
    public Optional<FaqEntry> bestMatch(String message) {
        return bestMatchWithScore(message).map(r -> r.entry);
    }

    /**
     * 语义匹配：计算用户问题与所有 FAQ 问题的余弦相似度，返回最匹配的
     */
    public Optional<FaqMatchResult> semanticMatch(String message) {
        try {
            List<Double> queryVector = aiChatClient.embed(message);
            if (queryVector == null || queryVector.isEmpty()) return Optional.empty();

            List<FaqEntry> allEntries = entryMapper.selectList(
                new QueryWrapper<FaqEntry>().eq("enabled", 1).isNotNull("question_vector"));

            if (allEntries.isEmpty()) return Optional.empty();

            List<FaqMatchResult> results = new ArrayList<>();
            for (FaqEntry entry : allEntries) {
                List<Double> entryVec = VectorUtils.fromJson(entry.questionVector);
                if (entryVec != null && !entryVec.isEmpty() && entryVec.size() == queryVector.size()) {
                    double score = VectorUtils.cosineSimilarity(queryVector, entryVec);
                    results.add(new FaqMatchResult(entry, score, "semantic"));
                }
            }

            return results.stream()
                .max(Comparator.comparingDouble(r -> r.score));
        } catch (Exception e) {
            // 语义检索失败，返回空，让关键词匹配兜底
            return Optional.empty();
        }
    }

    /**
     * 为所有没有向量的 FAQ 批量生成向量（管理后台可调用）
     */
    public int rebuildVectors() {
        List<FaqEntry> entries = entryMapper.selectList(
            new QueryWrapper<FaqEntry>().eq("enabled", 1));
        int count = 0;
        for (FaqEntry entry : entries) {
            if (!StringUtils.hasText(entry.questionVector)) {
                generateVector(entry);
                entryMapper.updateById(entry);
                count++;
            }
        }
        return count;
    }

    // ========== 内部方法 ==========

    private void generateVector(FaqEntry entry) {
        if (!StringUtils.hasText(entry.question)) return;
        try {
            List<Double> vec = aiChatClient.embed(entry.question);
            entry.questionVector = VectorUtils.toJson(vec);
        } catch (Exception ignored) {
            // 向量生成失败不影响主流程
        }
    }

    private void applyEntry(FaqEntry entry, Map<String, Object> request, boolean creating) {
        if (creating || request.containsKey("question")) entry.question = required(request, "question");
        if (creating || request.containsKey("answer")) entry.answer = required(request, "answer");
        if (request.containsKey("categoryId")) entry.categoryId = longValue(request.get("categoryId"));
        if (request.containsKey("keywords")) entry.keywords = stringValue(request.get("keywords"));
        if (request.containsKey("enabled")) {
            entry.enabled = boolValue(request.get("enabled")) ? 1 : 0;
        } else if (creating) {
            entry.enabled = 1;
        }
    }

    // ========== 匹配结果 ==========

    public static class FaqMatchResult {
        public final FaqEntry entry;
        public final double score;
        public final String matchType; // semantic / keyword / fuzzy

        public FaqMatchResult(FaqEntry entry, double score, String matchType) {
            this.entry = entry;
            this.score = score;
            this.matchType = matchType;
        }

        public FaqEntry getEntry() { return entry; }
        public double getScore() { return score; }
        public String getMatchType() { return matchType; }
    }

    // ========== 工具方法 ==========

    private String required(Map<String, Object> request, String key) {
        String value = stringValue(request.get(key));
        if (!StringUtils.hasText(value)) throw new BusinessException("缺少必填字段: " + key);
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        return value == null || !StringUtils.hasText(value.toString()) ? null : Long.valueOf(value.toString());
    }

    private Integer intValue(Object value, int fallback) {
        return value == null || !StringUtils.hasText(value.toString()) ? fallback : Integer.valueOf(value.toString());
    }

    private boolean boolValue(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return value != null && ("1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString()));
    }
}
