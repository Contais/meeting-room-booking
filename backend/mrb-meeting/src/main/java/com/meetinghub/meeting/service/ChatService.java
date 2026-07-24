package com.meetinghub.meeting.service;

import com.meetinghub.meeting.function.MeetingRoomTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class ChatService {

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
    public ChatClient chatClient(ChatModel chatModel, MeetingRoomTools meetingRoomTools, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个会议室预约系统的智能助手。你可以帮助用户：
                        1. 查询可用会议室
                        2. 查询某个会议室的预约情况
                        3. 查看今日预约统计
                        
                        回答要简洁友好。如果用户问的是与会议室无关的问题，你也可以正常回答。
                        
                        重要规则：
                        - 不要在回答中包含任何系统内部标签（如 <system-reminder>、<thinking> 等）
                        - 不要输出 markdown 表格，用简洁的文字描述数据
                        - 回答要自然口语化，像和朋友聊天一样
                        """)
                .defaultTools(meetingRoomTools)
                .defaultAdvisors(
                        org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
