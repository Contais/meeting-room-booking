package com.meetinghub.meeting.config;

import com.meetinghub.meeting.tools.common.CommonTool;
import com.meetinghub.meeting.tools.knowledge.KnowledgeTool;
import com.meetinghub.meeting.tools.meeting.MeetingRoomTool;
import com.meetinghub.meeting.tools.reservation.ReservationTool;
import com.meetinghub.meeting.tools.support.LoggingToolCallbackProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
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
 * - 使用 Redis ChatMemory 管理会话上下文（多实例共享、重启不丢失）
 * </p>
 */
@Configuration
public class SpringAIConfiguration {

    /**
     * 基于 Redis 的聊天记忆（按 sessionId 隔离会话，多实例共享）
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    /**
     * 构建 ChatClient，注入系统提示词、会话记忆和 Function Calling 工具
     *
     * @param meetingRoomTool 会议室相关工具（查询会议室、预约统计等）
     * @param reservationTool 预约相关工具（创建/取消预约、参会人邀请等）
     * @param commonTool      通用工具（查询天气、时间等）
     * @param knowledgeTool   知识库工具（检索规则/流程/公告等非结构化知识）
     * @param chatMemory      聊天记忆
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory,
                                 MeetingRoomTool meetingRoomTool, ReservationTool reservationTool, CommonTool commonTool,
                                 KnowledgeTool knowledgeTool,
                                 @Value("classpath:prompt/chatbot-system-prompt.md") Resource systemPrompt) throws IOException {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultToolCallbacks(new LoggingToolCallbackProvider(meetingRoomTool, reservationTool, commonTool, knowledgeTool))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
