package com.meetinghub.meeting.service;

import reactor.core.publisher.Flux;

/**
 * AI 聊天服务
 */
public interface ChatService {

    /**
     * 流式对话：注入会话记忆与工具上下文，调用模型并流式返回
     *
     * @param message        用户消息
     * @param userId         当前用户 ID（由 Controller 在请求线程捕获传入）
     * @param role           当前用户角色
     * @param conversationId 客户端会话 ID（未登录校验由 Controller 负责）
     */
    Flux<String> stream(String message, Long userId, String role, String conversationId);

    /**
     * 清空指定用户的会话记忆
     */
    void clearSession(Long userId, String sessionId);
}
