package com.meetinghub.meeting.service;

import com.meetinghub.meeting.function.MeetingRoomTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 聊天服务
 * <p>
 * 职责：
 * 1. 管理会话上下文（每个 sessionId 维护一份对话历史）
 * 2. 构建 ChatClient（注入 System Prompt + Function Calling 工具）
 * 3. 提供会话清理能力
 * </p>
 */
@Service
public class ChatService {

    private final ChatModel chatModel;
    private final MeetingRoomTools meetingRoomTools;
    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    /** 系统提示词：定义 AI 助手的角色和能力边界 */
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

    /**
     * 构建 ChatClient 实例
     * <p>
     * 注入系统提示词和 Function Calling 工具（MeetingRoomTools），
     * AI 可根据用户意图自动决定是否调用这些工具。
     * </p>
     */
    public ChatClient getChatClient() {
        // 将 MeetingRoomTools 转换为 Spring AI 的 ToolCallback 数组
        ToolCallback[] toolCallbacks = ToolCallbacks.from(meetingRoomTools);
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(toolCallbacks)
                .build();
    }

    /**
     * 获取或创建会话的对话历史
     *
     * @param sessionId 会话标识（前端生成的 UUID）
     * @return 当前会话的消息列表
     */
    public List<Message> getOrCreateSession(String sessionId) {
        return sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    /**
     * 清空指定会话的对话历史
     *
     * @param sessionId 会话标识
     */
    public void clearSession(String sessionId) {
        sessionHistories.remove(sessionId);
    }
}
