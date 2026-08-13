package com.meetinghub.meeting.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetinghub.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的 AI 会话记忆存储
 * <p>
 * Spring AI 官方 RedisChatMemoryRepository 仅随 2.0（要求 Spring Boot 4.x）发布，
 * 当前项目基于 Spring AI 1.1.3，自研实现 {@link ChatMemoryRepository}：
 * <ul>
 *   <li>Key：{@code mrb:chat:memory:{conversationId}}（统一 mrb: 前缀）</li>
 *   <li>Value：{@link StoredChatMessage} JSON 数组，每次全量替换</li>
 *   <li>TTL：7 天，活跃会话每次写入自动续期，无需定时清理任务</li>
 *   <li>多实例共享、重启不丢失</li>
 * </ul>
 * Redis 故障时降级：读写失败仅记日志，不阻断对话主流程。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * 会话记忆 TTL：7 天
     */
    static final Duration MEMORY_TTL = Duration.ofDays(7);

    private static final TypeReference<List<StoredChatMessage>> MESSAGE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        try {
            Set<String> keys = redisTemplate.keys(RedisKeyConstant.CHAT_MEMORY + "*");
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            return keys.stream()
                    .map(key -> key.substring(RedisKeyConstant.CHAT_MEMORY.length()))
                    .toList();
        } catch (Exception e) {
            log.warn("Redis 会话 ID 查询失败", e);
            return List.of();
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        try {
            String json = redisTemplate.opsForValue().get(key(conversationId));
            if (!StringUtils.hasText(json)) {
                return List.of();
            }
            return objectMapper.readValue(json, MESSAGE_LIST_TYPE).stream()
                    .map(StoredChatMessage::toMessage)
                    .toList();
        } catch (Exception e) {
            log.warn("Redis 会话记忆读取失败, conversationId={}", conversationId, e);
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            List<StoredChatMessage> stored = messages.stream()
                    .map(StoredChatMessage::from)
                    .toList();
            String json = objectMapper.writeValueAsString(stored);
            redisTemplate.opsForValue().set(key(conversationId), json, MEMORY_TTL);
        } catch (Exception e) {
            // 记忆写入失败降级：不阻断对话，仅记录日志
            log.warn("Redis 会话记忆写入失败, conversationId={}, messageCount={}",
                    conversationId, messages.size(), e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            redisTemplate.delete(key(conversationId));
        } catch (Exception e) {
            log.warn("Redis 会话记忆删除失败, conversationId={}", conversationId, e);
        }
    }

    private String key(String conversationId) {
        return RedisKeyConstant.CHAT_MEMORY + conversationId;
    }
}
