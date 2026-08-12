package com.aics.ticket.kb;

import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.kb.mapper.FaqFeedbackMapper;
import com.aics.ticket.kb.mapper.FaqTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FaqController {

    private final FaqService faqService;
    private final FaqTagMapper tagMapper;
    private final FaqFeedbackMapper feedbackMapper;

    @Value("${app.faq.similarity-threshold:0.72}")
    private double similarityThreshold;

    @Value("${app.faq.semantic-search-enabled:true}")
    private boolean semanticSearchEnabled;

    public FaqController(FaqService faqService, FaqTagMapper tagMapper, FaqFeedbackMapper feedbackMapper) {
        this.faqService = faqService;
        this.tagMapper = tagMapper;
        this.feedbackMapper = feedbackMapper;
    }

    // ========== 用户端 FAQ ==========

    @GetMapping("/faq/categories")
    public ApiResponse<List<FaqCategory>> categories() {
        return ApiResponse.ok(faqService.categories());
    }

    @GetMapping("/faq/entries")
    public ApiResponse<List<FaqEntry>> entries(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(faqService.entries(keyword, categoryId, true));
    }

    @GetMapping("/faq/match")
    public ApiResponse<Map<String, Object>> match(@RequestParam String question) {
        return matchResult(faqService.bestMatchWithScore(question));
    }

    /** 用户提交 FAQ 反馈（有帮助/无帮助） */
    @PostMapping("/faq/{entryId}/feedback")
    public ApiResponse<Void> submitFeedback(@PathVariable Long entryId,
                                            @RequestBody Map<String, Object> request) {
        FaqFeedback feedback = new FaqFeedback();
        feedback.entryId = entryId;
        // 兼容 boolean true / 字符串 "true" / 数字 1 三种前端传值方式
        Object rawHelpful = request.get("helpful");
        feedback.helpful = rawHelpful instanceof Boolean b ? b
                : "true".equalsIgnoreCase(String.valueOf(rawHelpful)) || "1".equals(String.valueOf(rawHelpful));
        feedback.userId = com.aics.ticket.auth.CurrentIdentity.currentIdOrDefault("USER", null);
        feedback.createdAt = LocalDateTime.now();
        feedbackMapper.insert(feedback);
        return ApiResponse.ok();
    }

    /** 查询 FAQ 反馈统计 */
    @GetMapping("/faq/{entryId}/feedback-stats")
    public ApiResponse<Map<String, Object>> feedbackStats(@PathVariable Long entryId) {
        Long helpful = feedbackMapper.selectCount(
            new QueryWrapper<FaqFeedback>().eq("entry_id", entryId).eq("helpful", true));
        Long notHelpful = feedbackMapper.selectCount(
            new QueryWrapper<FaqFeedback>().eq("entry_id", entryId).eq("helpful", false));
        Map<String, Object> stats = new HashMap<>();
        stats.put("entryId", entryId);
        stats.put("helpful", helpful);
        stats.put("notHelpful", notHelpful);
        return ApiResponse.ok(stats);
    }

    // ========== 管理端 FAQ 分类 ==========

    @GetMapping("/admin/faq/categories")
    public ApiResponse<List<FaqCategory>> adminCategories() {
        return ApiResponse.ok(faqService.categories());
    }

    @PostMapping("/admin/faq/categories")
    public ApiResponse<FaqCategory> createCategory(@RequestBody Map<String, Object> request) {
        return ApiResponse.ok(faqService.createCategory(request));
    }

    @PutMapping("/admin/faq/categories/{id}")
    public ApiResponse<FaqCategory> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(faqService.updateCategory(id, request));
    }

    @DeleteMapping("/admin/faq/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        faqService.deleteCategory(id);
        return ApiResponse.ok();
    }

    // ========== 管理端 FAQ 标签 ==========

    @GetMapping("/admin/faq/tags")
    public ApiResponse<List<FaqTag>> adminTags() {
        return ApiResponse.ok(tagMapper.selectList(new QueryWrapper<FaqTag>().orderByAsc("id")));
    }

    @PostMapping("/admin/faq/tags")
    public ApiResponse<FaqTag> createTag(@RequestBody Map<String, Object> request) {
        String name = request.get("name") != null ? request.get("name").toString().trim() : "";
        if (name.isEmpty()) {
            return ApiResponse.fail(400, "标签名不能为空");
        }
        FaqTag tag = new FaqTag();
        tag.name = name;
        tagMapper.insert(tag);
        return ApiResponse.ok(tag);
    }

    @DeleteMapping("/admin/faq/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        tagMapper.deleteById(id);
        return ApiResponse.ok();
    }

    // ========== 管理端 FAQ 问答 ==========

    @GetMapping("/admin/faq/entries")
    public ApiResponse<Page<FaqEntry>> adminEntries(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(faqService.entriesPage(page, size, keyword, categoryId, false));
    }

    @GetMapping("/admin/faq/entries/{id}")
    public ApiResponse<FaqEntry> getEntry(@PathVariable Long id) {
        return ApiResponse.ok(faqService.getEntry(id));
    }

    @PostMapping("/admin/faq/entries")
    public ApiResponse<FaqEntry> createEntry(@RequestBody Map<String, Object> request) {
        return ApiResponse.ok(faqService.createEntry(request));
    }

    @PutMapping("/admin/faq/entries/{id}")
    public ApiResponse<FaqEntry> updateEntry(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(faqService.updateEntry(id, request));
    }

    @DeleteMapping("/admin/faq/entries/{id}")
    public ApiResponse<Void> deleteEntry(@PathVariable Long id) {
        faqService.deleteEntry(id);
        return ApiResponse.ok();
    }

    /** 批量导入 FAQ（CSV 文本，首行表头：question,answer,keywords,categoryId） */
    @PostMapping("/admin/faq/entries/import")
    public ApiResponse<Map<String, Object>> importEntries(@RequestBody Map<String, String> request) {
        String csv = request.get("csv");
        if (csv == null || csv.isBlank()) {
            return ApiResponse.fail(400, "请提供 CSV 内容");
        }
        String[] lines = csv.strip().split("\n");
        if (lines.length < 2) return ApiResponse.fail(400, "CSV 至少需要表头和一条数据");
        int imported = 0, skipped = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) { skipped++; continue; }
            String[] cols = line.split(",", -1);
            if (cols.length < 2) { skipped++; continue; }
            try {
                Map<String, Object> req = new HashMap<>();
                req.put("question", cols[0].strip());
                req.put("answer", cols.length > 1 ? cols[1].strip() : "");
                if (cols.length > 2 && !cols[2].isBlank()) req.put("keywords", cols[2].strip());
                if (cols.length > 3 && !cols[3].isBlank()) req.put("categoryId", Long.valueOf(cols[3].strip()));
                faqService.createEntry(req);
                imported++;
            } catch (Exception e) { skipped++; }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        return ApiResponse.ok(result);
    }

    /** 导出 FAQ 为 CSV */
    @GetMapping("/admin/faq/entries/export")
    public ResponseEntity<byte[]> exportEntries() {
        List<FaqEntry> entries = faqService.entries(null, null, false);
        StringBuilder sb = new StringBuilder("﻿"); // BOM for Excel
        sb.append("question,answer,keywords,categoryId,enabled\n");
        for (FaqEntry e : entries) {
            sb.append(csvEscape(e.question)).append(",");
            sb.append(csvEscape(e.answer)).append(",");
            sb.append(csvEscape(e.keywords)).append(",");
            sb.append(e.categoryId != null ? e.categoryId : "").append(",");
            sb.append(e.enabled == 1 ? 1 : 0).append("\n");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "faq-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /** 动态读取 FAQ 配置 */
    @GetMapping("/admin/faq/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("similarityThreshold", similarityThreshold);
        config.put("semanticSearchEnabled", semanticSearchEnabled);
        return ApiResponse.ok(config);
    }

    // ========== 向量管理 ==========

    @PostMapping("/admin/faq/vectors/rebuild")
    public ApiResponse<Map<String, Object>> rebuildVectors() {
        int count = faqService.rebuildVectors();
        return ApiResponse.ok(Map.of("rebuilt", count));
    }

    @GetMapping("/admin/faq/match-test")
    public ApiResponse<Map<String, Object>> matchTest(@RequestParam String question) {
        return matchResult(faqService.bestMatchWithScore(question));
    }

    // ========== 内部 ==========

    private ApiResponse<Map<String, Object>> matchResult(Optional<FaqService.FaqMatchResult> result) {
        Map<String, Object> data = new HashMap<>();
        if (result.isPresent()) {
            FaqService.FaqMatchResult r = result.get();
            data.put("matched", true);
            data.put("faqId", r.entry.id);
            data.put("question", r.entry.question);
            data.put("answer", r.entry.answer);
            data.put("score", r.score);
            data.put("matchType", r.matchType);
        } else {
            data.put("matched", false);
        }
        return ApiResponse.ok(data);
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
