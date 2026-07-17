package com.aics.ticket.chat;

import com.aics.ticket.ai.AiChatClient;
import com.aics.ticket.auth.CurrentIdentity;
import com.aics.ticket.common.ApiResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {

    private final AiChatClient aiChatClient;
    private final ViolationGuardService violationGuardService;
    private final ChatService chatService;

    @Value("${app.ai.stream-timeout-seconds:120}")
    private int streamTimeoutSeconds;

    // 使用固定线程池处理流式请求，避免每次 new Thread
    private final ExecutorService streamExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        r -> {
            Thread t = new Thread(r, "chat-stream");
            t.setDaemon(true);
            return t;
        }
    );

    public ChatController(AiChatClient aiChatClient, ViolationGuardService violationGuardService, ChatService chatService) {
        this.aiChatClient = aiChatClient;
        this.violationGuardService = violationGuardService;
        this.chatService = chatService;
    }

    @PostMapping("/chat/conversations")
    public ApiResponse<Map<String, Object>> createConversation() {
        ChatConversationEntity conversation = chatService.createConversation();
        return ApiResponse.ok(Map.of("conversationId", conversation.id, "status", conversation.status));
    }

    @GetMapping("/chat/conversations")
    public ApiResponse<List<ChatConversationEntity>> conversations() {
        return ApiResponse.ok(chatService.listConversations());
    }

    @GetMapping("/chat/conversations/{id}/messages")
    public ApiResponse<List<ChatMessageEntity>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.messages(id));
    }

    /**
     * 非流式对话接口
     */
    @PostMapping("/chat/messages")
    public ApiResponse<Map<String, Object>> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        violationGuardService.check(request.getMessage());
        return ApiResponse.ok(chatService.send(request));
    }

    /**
     * 流式对话接口（SSE）
     * 事件流：message（token） → ... → done
     */
    @GetMapping("/chat/messages/stream")
    public SseEmitter streamMessage(@RequestParam String message,
                                     @RequestParam(required = false) Long conversationId) throws IOException {
        violationGuardService.check(message);

        // 在 web 线程中获取用户ID，异步线程拿不到 sa-token 上下文
        Long userId = CurrentIdentity.currentIdOrDefault("USER", 1L);

        long timeoutMs = streamTimeoutSeconds * 1000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);

        // 超时回调：优雅结束，避免抛 500
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("请求超时，请稍后再试"));
                emitter.complete();
            } catch (IOException ignored) {}
        });

        // 错误回调
        emitter.onError(e -> {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (IOException ignored) {}
        });

        // 异步处理流式回复
        streamExecutor.submit(() -> {
            try {
                chatService.sendStream(message, conversationId, userId,
                    token -> {
                        try {
                            emitter.send(SseEmitter.event().name("message").data(token));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    (finalConvId, finalAnswer) -> {
                        try {
                            emitter.send(SseEmitter.event().name("done")
                                .data(Map.of("conversationId", finalConvId, "answer", finalAnswer)));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ignored) {}
            }
        });

        return emitter;
    }

    /**
     * 兼容旧版流式接口
     */
    @GetMapping("/chat/stream")
    public SseEmitter stream(@RequestParam String message) throws IOException {
        return streamMessage(message, null);
    }

    @PostMapping("/chat/transfer-human")
    public ApiResponse<Map<String, Object>> transferHuman(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.ok(chatService.transferHuman(request));
    }

    @DeleteMapping("/chat/conversations/{id}")
    public ApiResponse<Map<String, Boolean>> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return ApiResponse.ok(Map.of("success", true));
    }
}
