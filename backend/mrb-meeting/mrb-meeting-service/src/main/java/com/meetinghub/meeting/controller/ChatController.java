package com.meetinghub.meeting.controller;

import com.meetinghub.common.context.UserContext;
import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.tools.support.ToolAuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天控制器
 * <p>
 * 参考 spring-ai-learning 项目实现：
 * - 返回 Flux<String>，Spring MVC 自动转为 SSE 流式响应
 * - 通过 ChatMemory.CONVERSATION_ID 实现会话隔离
 * - AI 可通过 Function Calling 自动调用后端接口
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    /**
     * SSE 流式对话接口
     * <p>
     * 前端发送用户消息，服务端以 SSE 流式逐块返回 AI 回答。
     * 通过 conversationId 隔离不同会话的上下文。
     * </p>
     * <p>
     * 在 Servlet 请求线程中（拦截器已填充 {@link UserContext}）捕获 userId/role，
     * 通过 {@code .toolContext(Map)} 显式传入 Flux 管道。AI 工具方法在异步线程
     * 执行时，通过 {@link com.meetinghub.meeting.tools.support.ToolAuthHelper} 从
     * {@link org.springframework.ai.chat.model.ToolContext} 读取用户身份，
     * 避免依赖 ThreadLocal（工具回调线程与请求线程不同，无法可靠传播）。
     * 会话记忆按 userId 隔离，Redis key 形如 {@code mrb:chat:memory:{userId}:{clientSessionId}}。
     * </p>
     */
    @PostMapping(value = "/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-Session-Id", required = false) String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        Long userId = UserContext.getCurrentUserId();
        // 会话记忆按用户隔离，避免跨用户复用会话 ID 读取他人上下文
        final String cid = scopedConversationId(userId, conversationId);

        // 在请求线程捕获用户身份（拦截器已设置 ThreadLocal），随后显式传入 Reactor 管道
        String role = UserContext.getCurrentRole();
        String message = request.getMessage();
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

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        return text.length() <= maxLength
                ? text
                : text.substring(0, maxLength) + "...(已截断, 共" + text.length() + "字符)";
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话 ID
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        Long userId = UserContext.getCurrentUserId();
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
}
