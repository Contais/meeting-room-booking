package com.meetinghub.meeting.service.impl;

import com.meetinghub.meeting.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    @Test
    void should_clearScopedMemory_when_clearSession() {
        ChatService service = new ChatServiceImpl(chatClient, chatMemory);

        service.clearSession(100L, "session-1");

        verify(chatMemory).clear("100:session-1");
    }

    @Test
    void should_streamWithUserScopedConversation_when_stream() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("你好")).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.advisors((Consumer<ChatClient.AdvisorSpec>) any())).thenAnswer(invocation -> {
            Consumer<ChatClient.AdvisorSpec> consumer = invocation.getArgument(0);
            ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
            consumer.accept(advisorSpec);
            verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, "100:session-1");
            return requestSpec;
        });
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("回复"));

        ChatService service = new ChatServiceImpl(chatClient, chatMemory);
        String result = service.stream("你好", 100L, "ROLE_USER", "session-1").blockLast();

        assertThat(result).isEqualTo("回复");
    }
}
