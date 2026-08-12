package com.aics.ticket.ticket;

import com.aics.ticket.common.ApiResponse;
import com.aics.ticket.common.PageResponse;
import com.aics.ticket.common.enums.TicketPriority;
import com.aics.ticket.common.enums.TicketSource;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ========== 用户端 ==========

    @PostMapping("/user/tickets")
    public ApiResponse<TicketRecord> createUserTicket(@Valid @RequestBody TicketCreateRequest request) {
        return ApiResponse.ok(ticketService.create(request, TicketSource.USER_FORM));
    }

    @GetMapping("/user/tickets")
    public ApiResponse<PageResponse<TicketRecord>> listUserTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(ticketService.listUserTickets(page, size, status, keyword));
    }

    @GetMapping("/user/tickets/{id}")
    public ApiResponse<TicketRecord> getUserTicket(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.get(id));
    }

    // ========== 坐席端 - 工单池 ==========

    @GetMapping("/agent/tickets/pool")
    public ApiResponse<PageResponse<TicketRecord>> ticketPool(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority) {
        return ApiResponse.ok(ticketService.pool(page, size, category, priority));
    }

    @GetMapping("/agent/tickets/mine")
    public ApiResponse<PageResponse<TicketRecord>> myTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(ticketService.mine(page, size, status, keyword));
    }

    @GetMapping("/agent/tickets/{id}")
    public ApiResponse<TicketRecord> getAgentTicket(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.get(id));
    }

    @PostMapping("/agent/tickets/{id}/accept")
    public ApiResponse<TicketRecord> accept(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String operator = request == null ? null : request.getOperator();
        return ApiResponse.ok(ticketService.accept(id, operator));
    }

    @PostMapping("/agent/tickets/{id}/process")
    public ApiResponse<TicketRecord> process(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String remark = request == null ? null : request.getRemark();
        return ApiResponse.ok(ticketService.process(id, remark));
    }

    @PostMapping("/agent/tickets/{id}/follow")
    public ApiResponse<TicketRecord> follow(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String remark = request == null ? null : request.getRemark();
        return ApiResponse.ok(ticketService.follow(id, remark));
    }

    @PostMapping("/agent/tickets/{id}/complete")
    public ApiResponse<TicketRecord> complete(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String remark = request == null ? null : request.getRemark();
        return ApiResponse.ok(ticketService.complete(id, remark));
    }

    @PostMapping("/agent/tickets/{id}/reject")
    public ApiResponse<TicketRecord> reject(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String remark = request == null ? null : request.getRemark();
        return ApiResponse.ok(ticketService.reject(id, remark));
    }

    /** 坐席归档已完结工单 */
    @PostMapping("/agent/tickets/{id}/archive")
    public ApiResponse<TicketRecord> archive(@PathVariable Long id,
            @RequestBody(required = false) TicketActionRequest request) {
        String remark = request == null ? null : request.getRemark();
        return ApiResponse.ok(ticketService.archive(id, remark));
    }

    /** 坐席申请调整工单优先级 */
    @PostMapping("/agent/tickets/{id}/adjust-priority")
    public ApiResponse<TicketRecord> adjustPriority(@PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newPriority = request.get("priority");
        String remark = request.getOrDefault("remark", null);
        if (newPriority == null || newPriority.isBlank()) {
            return ApiResponse.fail(400, "请指定新优先级");
        }
        return ApiResponse.ok(ticketService.adjustPriority(id, newPriority, remark));
    }

    // ========== 超时预警 ==========

    @GetMapping("/agent/tickets/warnings")
    public ApiResponse<List<TicketRecord>> timeoutWarnings() {
        return ApiResponse.ok(ticketService.timeoutWarnings());
    }

    // ========== 管理端 ==========

    @GetMapping("/admin/tickets")
    public ApiResponse<PageResponse<TicketRecord>> adminTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long agentId) {
        return ApiResponse.ok(ticketService.adminPage(page, size, status, category, priority, keyword, agentId));
    }

    @GetMapping("/admin/tickets/{id}")
    public ApiResponse<TicketRecord> getAdminTicket(@PathVariable Long id) {
        return ApiResponse.ok(ticketService.get(id));
    }

    @PutMapping("/admin/tickets/{id}")
    public ApiResponse<TicketRecord> updateAdminTicket(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(ticketService.adminUpdate(id, request));
    }

    /** 管理端导出工单 CSV */
    @GetMapping("/admin/tickets/export")
    public ResponseEntity<byte[]> exportTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long agentId) {
        String csv = ticketService.exportCsv(status, category, priority, keyword, agentId);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "tickets-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    // ========== 统计数据（管理端 & 坐席端都能用） ==========

    @GetMapping({"/admin/tickets/overview", "/agent/tickets/overview"})
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(ticketService.overview());
    }

    @GetMapping({"/admin/tickets/trend", "/agent/tickets/trend"})
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(ticketService.trend(days));
    }

    @GetMapping({"/admin/tickets/status-distribution", "/agent/tickets/status-distribution"})
    public ApiResponse<List<Map<String, Object>>> statusDistribution() {
        return ApiResponse.ok(ticketService.statusDistribution());
    }

    @GetMapping({"/admin/tickets/category-distribution", "/agent/tickets/category-distribution"})
    public ApiResponse<List<Map<String, Object>>> categoryDistribution() {
        return ApiResponse.ok(ticketService.categoryDistribution());
    }
}
