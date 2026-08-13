package com.meetinghub.meeting.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    @Test
    void should_clearMemory_when_clearSession() {
        ChatController controller = new ChatController(chatClient, chatMemory);

        controller.clearSession("session-1");

        verify(chatMemory).clear("session-1");
    }
}
