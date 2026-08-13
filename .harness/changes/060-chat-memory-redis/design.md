# 060-chat-memory-redis 技术方案

## 背景

现状：`SpringAIConfiguration.chatMemory()` 使用 `MessageWindowChatMemory` +
`InMemoryChatMemoryRepository`（ConcurrentHashMap），重启即丢、多实例不共享；
`ChatController.clearSession` 为空实现；前端每次加载页面随机生成 sessionId，刷新即断会话。

Spring AI 2.0 的官方 `RedisChatMemoryRepository` 依赖 Spring Boot 4.x，与当前
Boot 3.2.5 + Spring Cloud Alibaba 2023.0.1.0 技术栈不兼容，故在现有 1.1.3 上自研。

## 方案设计

### 存储结构

- Key：`mrb:chat:memory:{conversationId}`（复用 mrb-common `RedisKeyConstant`，符合红线前缀 `mrb:`）
- Value：JSON 数组，元素 `{"type":"USER|ASSISTANT|SYSTEM|TOOL","content":"..."}`
- TTL：7 天（`Duration.ofDays(7)`），每次 `saveAll` 重写 Key 时自动续期，无需定时清理任务

### 消息序列化

与官方 `JdbcChatMemoryRepository`（1.1.3）行为对齐：
- 仅持久化 `message.getText()` 与 `message.getMessageType()`
- TOOL 消息内容存空，读取时构造空 `ToolResponseMessage`
- 读取顺序即 JSON 数组顺序（`MessageWindowChatMemory` 每次全量替换，天然保序）

### 类设计

```java
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    // findConversationIds / findByConversationId / saveAll / deleteByConversationId
}
```

- `findConversationIds()`：`keys(mrb:chat:memory:*)`（会话量小，demo 可接受）
- `findByConversationId()`：缺失/解析失败返回空列表
- `saveAll()`：全量替换 + 设置 TTL；序列化失败记日志降级，不阻断对话
- `deleteByConversationId()`：删除 Key（`clearSession` 使用）

### 接线与前端

- `SpringAIConfiguration.chatMemory()` 改为注入 `ChatMemoryRepository`（即 Redis 实现），
  保持 `MessageWindowChatMemory.maxMessages=20` 窗口策略不变
- `ChatController` 注入 `ChatMemory`，`clearSession` 调用 `chatMemory.clear(sessionId)`
- `ChatPanel.vue`：sessionId 改为 localStorage 读写（key: `mrb_chat_session_id`），
  `clearChat` 同时移除 localStorage 并调用 DELETE

## 失败策略

| 场景 | 行为 |
|------|------|
| Redis 连接异常 | 读写失败记日志，返回空记忆/跳过持久化，聊天主流程不受影响 |
| JSON 反序列化失败 | 返回空列表，本会话重新开始积累 |
| 会话过期（TTL） | 读取为空，等同新会话 |

## 测试计划

- `RedisChatMemoryRepositoryTest`：四类消息 round-trip、TTL 设置、缺失 Key、删除、keys 查询、畸形 JSON
- `ChatControllerTest`：`clearSession` 调用 `chatMemory.clear(sessionId)`
- 本机 Homebrew JDK 无法 self-attach，沿用网关模块做法：`mock-maker-subclass` 配置
