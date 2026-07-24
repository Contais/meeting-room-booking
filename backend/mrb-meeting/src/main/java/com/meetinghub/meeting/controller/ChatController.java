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

@RestController
@RequestMapping("/meeting/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request,
                                  @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        String finalSessionId = sessionId;

        CompletableFuture.runAsync(() -> {
            try {
                var messages = chatService.getOrCreateSession(finalSessionId);
                UserMessage userMessage = new UserMessage(request.getMessage());
                messages.add(userMessage);

                StringBuilder fullResponse = new StringBuilder();
                chatService.getChatClient()
                        .prompt()
                        .messages(messages)
                        .stream()
                        .content()
                        .subscribe(
                            chunk -> {
                                try {
                                    fullResponse.append(chunk);
                                    emitter.send(SseEmitter.event().data(chunk));
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            error -> {
                                try {
                                    emitter.send(SseEmitter.event().name("error").data("出错了: " + error.getMessage()));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
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

    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
    }
}
