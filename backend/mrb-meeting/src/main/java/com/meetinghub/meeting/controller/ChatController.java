package com.meetinghub.meeting.controller;

import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI 聊天控制器
 * <p>
 * 提供 SSE 流式对话接口。
 * 前端通过 POST /meeting/chat/stream 发送消息，服务端以流式方式逐步返回 AI 回答。
 * </p>
 * <p>
 * 注意：由于 Spring AI 1.1.3 的 Reactor 流式 API 在 Servlet 环境下存在
 * context-propagation 兼容性问题，当前采用阻塞式 call() + SSE 分块发送的方式实现。
 * </p>
 */
@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 对话接口（阻塞式 + SSE 模拟流式）
     * <p>
     * 工作流程：
     * 1. 根据 sessionId 获取或创建会话上下文
     * 2. 通过 ChatClient 阻塞式获取完整回答
     * 3. 将回答按字符逐块推送给前端，模拟流式效果
     * </p>
     *
     * @param request   包含用户消息的请求体
     * @param sessionId 会话 ID（可选，为空则自动生成）
     * @return SseEmitter 流式响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request,
                                  @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        String finalSessionId = sessionId;

        // 异步执行，避免阻塞 Servlet 线程
        CompletableFuture.runAsync(() -> {
            try {
                var messages = chatService.getOrCreateSession(finalSessionId);

                // 调用 AI 获取完整回答（阻塞式）
                String answer = chatService.getChatClient()
                        .prompt()
                        .messages(messages)
                        .call()
                        .content();

                // 保存到会话历史
                messages.add(new AssistantMessage(answer));

                // 模拟流式：按字符逐块发送
                for (int i = 0; i < answer.length(); i++) {
                    emitter.send(SseEmitter.event().data(String.valueOf(answer.charAt(i))));
                    // 小延迟让前端有时间渲染
                    if (i % 3 == 0) {
                        Thread.sleep(20);
                    }
                }

                // 发送结束信号
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务异常: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> emitter.complete());
        return emitter;
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话 ID
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
    }
}
