package com.meetinghub.meeting.controller;

import com.meetinghub.common.context.UserContext;
import com.meetinghub.meeting.model.dto.ChatRequest;
import com.meetinghub.meeting.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Test
    void should_delegateClearSession_when_clearSession() {
        UserContext.set(UserContext.of("100", "ROLE_USER", "alice"));
        try {
            ChatController controller = new ChatController(chatService);

            controller.clearSession("session-1");

            verify(chatService).clearSession(100L, "session-1");
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void should_delegateStream_when_chatStream() {
        UserContext.set(UserContext.of("100", "ROLE_USER", "alice"));
        try {
            when(chatService.stream("你好", 100L, "ROLE_USER", "session-1"))
                    .thenReturn(Flux.just("回复"));
            ChatController controller = new ChatController(chatService);
            ChatRequest request = new ChatRequest();
            request.setMessage("你好");

            String result = controller.chatStream(request, "session-1").blockLast();

            assertThat(result).isEqualTo("回复");
        } finally {
            UserContext.clear();
        }
    }
}
