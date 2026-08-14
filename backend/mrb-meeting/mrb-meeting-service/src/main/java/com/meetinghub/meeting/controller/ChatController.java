package com.meetinghub.meeting.controller;

import com.meetinghub.common.context.UserContext;
import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器
 * <p>
 * 仅负责请求参数归一化与用户身份捕获，业务编排（会话记忆、工具上下文、
 * 流式模型调用）下沉到 {@link ChatService}，保持 Controller 薄层。
 * </p>
 */
@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "流式对话与会话管理")
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 流式对话接口
     * <p>
     * 在 Servlet 请求线程中（拦截器已填充 {@link UserContext}）捕获 userId/role，
     * 以方法参数传入 Service，避免 Service 耦合 ThreadLocal。
     * </p>
     */
    @Operation(summary = "流式对话")
    @PostMapping(value = "/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-Session-Id", required = false) String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        return chatService.stream(request.getMessage(), UserContext.getCurrentUserId(),
                UserContext.getCurrentRole(), conversationId);
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话 ID
     */
    @Operation(summary = "清空会话历史")
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        chatService.clearSession(UserContext.getCurrentUserId(), sessionId);
    }
}
