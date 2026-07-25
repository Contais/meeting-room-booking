package com.meetinghub.meeting.config;

import com.meetinghub.meeting.function.MeetingRoomTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AI 聊天配置
 * <p>
 * 参考 spring-ai-learning 项目的实现方式：
 * - 使用 @Configuration + @Bean 注册 ChatClient
 * - 使用 defaultTools() 注册 Function Calling 工具
 * - 使用 InMemoryChatMemory 管理会话上下文
 * </p>
 */
@Configuration
public class SpringAIConfiguration {

/**
     * 内存聊天记忆（按 sessionId 隔离会话）
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /**
     * 构建 ChatClient，注入系统提示词、会话记忆和 Function Calling 工具
     *
     * @param meetingRoomTools 会议室相关工具（查询会议室、预约统计等）
     * @param chatMemory       聊天记忆
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, MeetingRoomTools meetingRoomTools, ChatMemory chatMemory,
                                  @Value("classpath:prompt/chatbot-system-prompt.md") Resource systemPrompt) throws IOException {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(meetingRoomTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
