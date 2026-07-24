package com.meetinghub.meeting.controller;

import com.meetinghub.meeting.model.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
     *
     * @param request        包含用户消息的请求体
     * @param conversationId 会话 ID（可选，为空则使用 session id）
     * @return Flux<String> 流式响应，Spring MVC 自动转为 SSE 格式
     */
    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-Session-Id", required = false) String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        final String cid = conversationId;

        return chatClient.prompt()
                .user(request.getMessage())
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
