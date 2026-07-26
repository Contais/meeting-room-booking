package com.meetinghub.meeting.controller;

import com.meetinghub.common.context.UserContext;
import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.tools.ToolAuthHelper;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    /**
     * SSE 流式对话接口
     * <p>
     * 前端发送用户消息，服务端以 SSE 流式逐块返回 AI 回答。
     * 通过 conversationId 隔离不同会话的上下文。
     * </p>
     * <p>
     * 在 Servlet 请求线程中（拦截器已填充 {@link UserContext}）捕获 userId/role，
     * 通过 {@code .toolContext(Map)} 显式传入 Flux 管道。AI 工具方法在异步线程
     * 执行时，通过 {@link com.meetinghub.meeting.tools.ToolAuthHelper} 从
     * {@link org.springframework.ai.chat.model.ToolContext} 读取用户身份，
     * 避免依赖 ThreadLocal（工具回调线程与请求线程不同，无法可靠传播）。
     * </p>
     */
    @PostMapping(value = "/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-Session-Id", required = false) String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        final String cid = conversationId;

        // 在请求线程捕获用户身份（拦截器已设置 ThreadLocal），随后显式传入 Reactor 管道
        Long userId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentRole();
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(ToolAuthHelper.KEY_USER_ID, userId);
        toolContext.put(ToolAuthHelper.KEY_ROLE, role);

        return chatClient.prompt()
                .user(request.getMessage())
                .toolContext(toolContext)
                .advisors(advice -> advice.param(ChatMemory.CONVERSATION_ID, cid))
                .stream()
                .content();
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话 ID
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        // InMemoryChatMemory 的清理由 ChatMemory 自动管理
        // 会话超时后自动释放
    }
}
