package com.meetinghub.meeting.service.impl;

import com.meetinghub.meeting.service.ChatService;
import com.meetinghub.meeting.tools.support.ToolAuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天服务实现
 * <p>
 * 负责会话记忆、工具上下文与流式模型调用的编排；会话 ID 按 userId 隔离，
 * Redis key 形如 {@code mrb:chat:memory:{userId}:{clientSessionId}}。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Override
    public Flux<String> stream(String message, Long userId, String role, String conversationId) {
        // 会话记忆按用户隔离，避免跨用户复用会话 ID 读取他人上下文
        String cid = scopedConversationId(userId, conversationId);
        log.info("AI 对话开始, conversationId={}, userId={}, message={}",
                cid, userId, truncate(message, 200));

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(ToolAuthHelper.KEY_USER_ID, userId);
        toolContext.put(ToolAuthHelper.KEY_ROLE, role);

        return chatClient.prompt()
                .user(message)
                .toolContext(toolContext)
                .advisors(advice -> advice.param(ChatMemory.CONVERSATION_ID, cid))
                .stream()
                .content()
                .doOnComplete(() -> log.info("AI 对话结束, conversationId={}, userId={}", cid, userId))
                .doOnError(e -> log.error("AI 对话异常, conversationId={}, userId={}", cid, userId, e));
    }

    @Override
    public void clearSession(Long userId, String sessionId) {
        chatMemory.clear(scopedConversationId(userId, sessionId));
        log.info("清空AI会话记忆, userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 将会话 ID 限定到当前用户，形成 {@code {userId}:{clientSessionId}}
     * <p>
     * 使 Redis key 可直接识别会话归属，同时隔离不同用户的会话上下文。
     * </p>
     */
    private String scopedConversationId(Long userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        return text.length() <= maxLength
                ? text
                : text.substring(0, maxLength) + "...(已截断, 共" + text.length() + "字符)";
    }
}
