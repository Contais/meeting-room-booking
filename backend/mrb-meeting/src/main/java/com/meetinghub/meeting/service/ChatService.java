package com.meetinghub.meeting.service;

import com.meetinghub.meeting.function.MeetingRoomTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final ChatModel chatModel;
    private final MeetingRoomTools meetingRoomTools;
    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            你是一个会议室预约系统的智能助手。你可以帮助用户：
            1. 查询可用会议室
            2. 查询某个会议室的预约情况
            3. 查看今日预约统计
            
            回答要简洁友好。如果用户问的是与会议室无关的问题，你也可以正常回答。
            """;

    public ChatService(ChatModel chatModel, MeetingRoomTools meetingRoomTools) {
        this.chatModel = chatModel;
        this.meetingRoomTools = meetingRoomTools;
    }

    public ChatClient getChatClient() {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(meetingRoomTools)
                .build();
    }

    public List<Message> getOrCreateSession(String sessionId) {
        return sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
    }
}
