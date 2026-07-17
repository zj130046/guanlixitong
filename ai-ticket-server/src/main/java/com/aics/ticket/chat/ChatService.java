package com.aics.ticket.chat;

import com.aics.ticket.ai.AiChatClient;
import com.aics.ticket.ai.DeepSeekAiChatClient;
import com.aics.ticket.auth.CurrentIdentity;
import com.aics.ticket.common.BusinessException;
import com.aics.ticket.common.enums.TicketPriority;
import com.aics.ticket.common.enums.TicketSource;
import com.aics.ticket.kb.FaqEntry;
import com.aics.ticket.kb.FaqService;
import com.aics.ticket.ticket.TicketCreateRequest;
import com.aics.ticket.ticket.TicketRecord;
import com.aics.ticket.ticket.TicketService;
import com.aics.ticket.chat.mapper.ChatConversationMapper;
import com.aics.ticket.chat.mapper.ChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final FaqService faqService;
    private final AiChatClient aiChatClient;
    private final TicketService ticketService;

    public ChatService(ChatConversationMapper conversationMapper, ChatMessageMapper messageMapper,
                       FaqService faqService, AiChatClient aiChatClient, TicketService ticketService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.faqService = faqService;
        this.aiChatClient = aiChatClient;
        this.ticketService = ticketService;
    }

    @Transactional
    public ChatConversationEntity createConversation() {
        ChatConversationEntity conversation = new ChatConversationEntity();
        conversation.userId = CurrentIdentity.currentIdOrDefault("USER", 1L);
        conversation.status = "AI_SERVING";
        conversationMapper.insert(conversation);
        return conversation;
    }

    public List<ChatConversationEntity> listConversations() {
        Long userId = CurrentIdentity.currentIdOrDefault("USER", 1L);
        return conversationMapper.selectList(new QueryWrapper<ChatConversationEntity>()
                .eq("user_id", userId)
                .orderByDesc("updated_at"));
    }

    @Transactional
    public void deleteConversation(Long id) {
        Long userId = CurrentIdentity.currentIdOrDefault("USER", 1L);
        ChatConversationEntity conversation = conversationMapper.selectById(id);
        if (conversation == null || !userId.equals(conversation.userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        messageMapper.delete(new QueryWrapper<ChatMessageEntity>().eq("conversation_id", id));
        conversationMapper.deleteById(id);
    }

    public List<ChatMessageEntity> messages(Long conversationId) {
        return messageMapper.selectList(new QueryWrapper<ChatMessageEntity>()
                .eq("conversation_id", conversationId)
                .orderByAsc("created_at"));
    }

    /**
     * 非流式发送
     */
    @Transactional
    public Map<String, Object> send(ChatMessageRequest request) {
        Long conversationId = request.getConversationId();
        ChatConversationEntity conversation = conversationId == null
            ? createConversation()
            : conversationMapper.selectById(conversationId);
        if (conversation == null) throw new BusinessException(404, "会话不存在");

        saveMessage(conversation.id, "USER", conversation.userId, request.getMessage());

        // 先查 FAQ
        FaqService.FaqMatchResult match = faqService.bestMatchWithScore(request.getMessage()).orElse(null);
        String answer;
        if (match != null) {
            answer = faqAnswer(match);
        } else {
            answer = aiChatClient.complete(request.getMessage());
        }

        saveMessage(conversation.id, "AI", null, answer);
        conversationMapper.updateById(conversation);

        return Map.of(
            "conversationId", conversation.id,
            "answer", answer,
            "matchType", match == null ? "ai" : match.matchType,
            "faqId", match == null ? null : match.entry.id
        );
    }

    /**
     * 流式发送：FAQ 命中直接返回，否则走 AI 流式逐 token 回调
     * 注意：此方法在异步线程中调用，Spring @Transactional 不生效（基于 ThreadLocal），
     *      每个数据库操作本身是单条事务，可独立提交。
     * @param message 用户消息
     * @param conversationId 会话ID（可选）
     * @param userId 用户ID（必须在 web 线程中获取后传入，异步线程中拿不到 sa-token 上下文）
     * @param onToken 每个 token 的回调
     * @param onComplete 完成时的回调，参数：会话ID、完整回答
     */
    public void sendStream(String message, Long conversationId, Long userId,
                           Consumer<String> onToken,
                           BiConsumer<Long, String> onComplete) {
        ChatConversationEntity conversation;
        if (conversationId == null) {
            conversation = new ChatConversationEntity();
            conversation.userId = userId;
            conversation.status = "AI_SERVING";
            conversationMapper.insert(conversation);
        } else {
            conversation = conversationMapper.selectById(conversationId);
        }
        if (conversation == null) throw new BusinessException(404, "会话不存在");

        saveMessage(conversation.id, "USER", conversation.userId, message);

        // 先查 FAQ
        FaqService.FaqMatchResult match = faqService.bestMatchWithScore(message).orElse(null);

        if (match != null) {
            // FAQ 命中：直接返回完整答案（也模拟流式输出，体验更好）
            String answer = faqAnswer(match);
            // 按句号/换行切分成小块模拟流式
            String[] chunks = answer.split("(?<=。|！|？|\n|；)");
            for (String chunk : chunks) {
                if (!chunk.isEmpty()) {
                    onToken.accept(chunk);
                    try { Thread.sleep(30); } catch (InterruptedException ignored) {}
                }
            }
            saveMessage(conversation.id, "AI", null, answer);
            conversationMapper.updateById(conversation);
            onComplete.accept(conversation.id, answer);
            return;
        }

        // 未命中 FAQ：走 AI 流式
        StringBuilder fullAnswer = new StringBuilder();
        if (aiChatClient instanceof DeepSeekAiChatClient deepSeek) {
            deepSeek.streamChat(message,
                token -> {
                    fullAnswer.append(token);
                    onToken.accept(token);
                },
                () -> {
                    saveMessage(conversation.id, "AI", null, fullAnswer.toString());
                    conversationMapper.updateById(conversation);
                    onComplete.accept(conversation.id, fullAnswer.toString());
                }
            );
        } else {
            // Mock 或其他实现：用 stream() 方法返回的列表模拟流式
            List<String> chunks = aiChatClient.stream(message);
            for (String chunk : chunks) {
                fullAnswer.append(chunk);
                onToken.accept(chunk);
                try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            }
            saveMessage(conversation.id, "AI", null, fullAnswer.toString());
            conversationMapper.updateById(conversation);
            onComplete.accept(conversation.id, fullAnswer.toString());
        }
    }

    @Transactional
    public Map<String, Object> transferHuman(Map<String, Object> request) {
        Long conversationId = null;
        if (request != null && request.get("conversationId") != null
                && StringUtils.hasText(request.get("conversationId").toString())) {
            conversationId = Long.valueOf(request.get("conversationId").toString());
        }
        ChatConversationEntity conversation = conversationId == null
            ? createConversation()
            : conversationMapper.selectById(conversationId);
        if (conversation == null) throw new BusinessException(404, "会话不存在");

        if (conversation.transferredTicketId != null) {
            return Map.of("conversationId", conversation.id,
                "ticketId", conversation.transferredTicketId, "status", conversation.status);
        }

        List<ChatMessageEntity> messages = messages(conversation.id);
        String firstUserMessage = messages.stream()
                .filter(m -> "USER".equals(m.senderType))
                .map(m -> m.content).findFirst()
                .orElse("AI 咨询转人工");
        String latestUserMessage = messages.stream()
                .filter(m -> "USER".equals(m.senderType))
                .reduce((f, s) -> s).map(m -> m.content).orElse(firstUserMessage);

        TicketCreateRequest ticketRequest = new TicketCreateRequest();
        Object titleValue = request == null ? null : request.get("title");
        ticketRequest.setTitle(titleValue != null && StringUtils.hasText(titleValue.toString())
                ? titleValue.toString() : trimTitle(firstUserMessage));
        ticketRequest.setDescription("来自 AI 会话 #" + conversation.id + " 的转人工请求：\n" + latestUserMessage);

        Object category = request != null ? request.get("category") : null;
        ticketRequest.setCategory(category != null ? category.toString() : "AI 咨询");
        Object department = request != null ? request.get("department") : null;
        ticketRequest.setDepartment(department != null ? department.toString() : "客服中心");

        ticketRequest.setPriority(TicketPriority.NORMAL);
        TicketRecord ticket = ticketService.createForUser(
            ticketRequest, TicketSource.AI_CHAT, conversation.userId, conversation.id);

        conversation.status = "WAITING_AGENT";
        conversation.transferredTicketId = ticket.getId();
        conversationMapper.updateById(conversation);
        saveMessage(conversation.id, "SYSTEM", null, "已转人工，工单号：" + ticket.getId());

        return Map.of("conversationId", conversation.id,
            "ticketId", ticket.getId(), "status", conversation.status);
    }

    // ========== 内部方法 ==========

    private void saveMessage(Long conversationId, String senderType, Long senderId, String content) {
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.conversationId = conversationId;
        msg.senderType = senderType;
        msg.senderId = senderId;
        msg.content = content;
        messageMapper.insert(msg);
    }

    private String faqAnswer(FaqService.FaqMatchResult match) {
        return "💡 根据知识库《" + match.entry.question + "》：\n\n" + match.entry.answer
            + "\n\n_匹配方式：" + matchTypeLabel(match.matchType) + "，相似度：" + Math.round(match.score * 100) + "%_";
    }

    private String matchTypeLabel(String type) {
        return switch (type) {
            case "semantic" -> "语义匹配";
            case "keyword" -> "关键词匹配";
            case "fuzzy" -> "模糊匹配";
            default -> "知识库";
        };
    }

    private String trimTitle(String text) {
        if (text == null) return "AI 咨询转人工";
        return text.length() > 30 ? text.substring(0, 30) + "..." : text;
    }
}
