package com.meetinghub.meeting.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

/**
 * AI 会话消息的轻量序列化载体
 * <p>
 * 与 Spring AI 官方 JDBC ChatMemoryRepository 行为对齐：仅持久化消息文本与类型；
 * 工具调用元数据与 ToolResponse 内容不持久化（同一轮的工具调用链由模型上下文保证）。
 * </p>
 */
public record StoredChatMessage(String type, String content) {

    /**
     * 将 Spring AI 消息转为可序列化载体
     */
    public static StoredChatMessage from(Message message) {
        String text = message.getText();
        return new StoredChatMessage(message.getMessageType().name(), text != null ? text : "");
    }

    /**
     * 还原为 Spring AI 消息；TOOL 消息与官方实现一致还原为空 ToolResponseMessage
     */
    public Message toMessage() {
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> ToolResponseMessage.builder().responses(List.of()).build();
        };
    }
}
