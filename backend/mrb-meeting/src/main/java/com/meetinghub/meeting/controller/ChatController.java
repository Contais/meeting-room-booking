package com.meetinghub.meeting.controller;

import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI 聊天控制器
 * <p>
 * 提供 SSE（Server-Sent Events）流式对话接口。
 * 前端通过 POST /meeting/chat/stream 发送消息，服务端以流式方式逐块返回 AI 回答。
 * 支持 Function Calling：AI 可自动调用会议室查询、预约统计等后端接口。
 * </p>
 */
@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 流式对话接口
     * <p>
     * 工作流程：
     * 1. 根据 sessionId 获取或创建会话上下文（内存中维护对话历史）
     * 2. 将用户消息加入历史，通过 Spring AI ChatClient 发起流式请求
     * 3. 通过 SseEmitter 逐块将 AI 回答推送给前端
     * 4. 流结束后将完整回答保存到会话历史，发送 [DONE] 信号
     * </p>
     *
     * @param request   包含用户消息的请求体
     * @param sessionId 会话 ID（可选，为空则自动生成）
     * @return SseEmitter 流式响应，事件类型：data=AI回答片段，error=错误，done=结束
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request,
                                  @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        // 未传 sessionId 时自动生成
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 设置 2 分钟超时，防止长时间无响应
        SseEmitter emitter = new SseEmitter(120_000L);
        String finalSessionId = sessionId;

        // 异步执行 AI 对话，避免阻塞 Servlet 线程
        CompletableFuture.runAsync(() -> {
            try {
                // 获取当前会话的对话历史
                var messages = chatService.getOrCreateSession(finalSessionId);
                UserMessage userMessage = new UserMessage(request.getMessage());
                messages.add(userMessage);

                StringBuilder fullResponse = new StringBuilder();

                // 通过 Spring AI ChatClient 发起流式请求，逐块订阅
                chatService.getChatClient()
                        .prompt()
                        .messages(messages)
                        .stream()
                        .content()
                        .subscribe(
                            // 每收到一块文本就推送给前端
                            chunk -> {
                                try {
                                    fullResponse.append(chunk);
                                    emitter.send(SseEmitter.event().data(chunk));
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            // 流式请求出错时通知前端
                            error -> {
                                try {
                                    emitter.send(SseEmitter.event().name("error").data("出错了: " + error.getMessage()));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            // 流式完成：保存助手回复到历史，发送结束信号
                            () -> {
                                try {
                                    var msgs = chatService.getOrCreateSession(finalSessionId);
                                    msgs.add(new AssistantMessage(fullResponse.toString()));
                                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            }
                        );
            } catch (Exception e) {
                // 异常兜底：通知前端服务异常
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务异常: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        // 超时和异常时自动关闭连接
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
