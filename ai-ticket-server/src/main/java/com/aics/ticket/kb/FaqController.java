package com.aics.ticket.kb;

import com.aics.ticket.common.ApiResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
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
        java.util.Optional<FaqService.FaqMatchResult> result = faqService.bestMatchWithScore(question);
        if (result.isPresent()) {
            FaqService.FaqMatchResult r = result.get();
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("matched", true);
            data.put("faqId", r.entry.id);
            data.put("question", r.entry.question);
            data.put("answer", r.entry.answer);
            data.put("score", r.score);
            data.put("matchType", r.matchType);
            return ApiResponse.ok(data);
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("matched", false);
        return ApiResponse.ok(data);
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

    /**
     * 重建所有 FAQ 的向量索引
     */
    @PostMapping("/admin/faq/vectors/rebuild")
    public ApiResponse<Map<String, Object>> rebuildVectors() {
        int count = faqService.rebuildVectors();
        return ApiResponse.ok(Map.of("rebuilt", count));
    }

    @GetMapping("/admin/faq/match-test")
    public ApiResponse<Map<String, Object>> matchTest(@RequestParam String question) {
        java.util.Optional<FaqService.FaqMatchResult> result = faqService.bestMatchWithScore(question);
        if (result.isPresent()) {
            FaqService.FaqMatchResult r = result.get();
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("matched", true);
            data.put("faqId", r.entry.id);
            data.put("question", r.entry.question);
            data.put("answer", r.entry.answer);
            data.put("score", r.score);
            data.put("matchType", r.matchType);
            return ApiResponse.ok(data);
        }
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("matched", false);
        return ApiResponse.ok(data);
    }
}
