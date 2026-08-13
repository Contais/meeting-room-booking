# 060-chat-memory-redis 需求分析摘要

## 需求描述

将 AI 助手会话记忆从 `InMemoryChatMemoryRepository`（重启即丢、多实例不共享）替换为
Redis 持久化存储，并修复 `clearSession` 空实现与前端会话 ID 不持久化的问题。

背景结论（已与用户确认）：
- Spring AI 官方 `RedisChatMemoryRepository` 仅存在于 2.0（要求 Spring Boot 4.x），
  当前项目锚定 Boot 3.2.5 + Spring Cloud Alibaba 2023.0.1.0，升级 2.0 属全家桶迁移，不采纳。
- 自研实现 `ChatMemoryRepository` 接口（4 个方法），成本可控，同时作为论文"Redis 会话存储"自研亮点。

## 验收标准

- [ ] 会话记忆写入 Redis，key 为 `mrb:chat:memory:{userId}:{clientSessionId}`，可直接识别归属用户
- [ ] 重启/多实例下会话上下文可恢复（Redis 共享存储）
- [ ] 会话记忆 TTL 7 天，活跃会话每次写入自动续期，无清理任务
- [ ] `DELETE /meeting/chat/session/{sessionId}` 真正清空 Redis 中的会话
- [ ] 前端用 sessionStorage 持久化会话 ID：误刷新不丢会话，关闭标签页/浏览器后重进为新会话
- [ ] "清空会话"同时清理本地与远端
- [ ] 工具调用消息（TOOL）与官方 JDBC 实现行为一致（内容存空）
- [ ] 单元测试通过（repository 读写/清空/异常 + clearSession 调用）

## 技术变更清单

| 变更 | 说明 |
|------|------|
| `RedisKeyConstant`（mrb-common） | 新增 `CHAT_MEMORY = mrb:chat:memory:` |
| `RedisChatMemoryRepository`（新增） | 实现 Spring AI `ChatMemoryRepository`，StringRedisTemplate + Jackson，TTL 7 天 |
| `StoredChatMessage`（新增） | 会话消息的轻量序列化载体（type + content） |
| `SpringAIConfiguration`（修改） | `chatMemory()` 注入 Redis 版 repository |
| `ChatController.clearSession`（修改） | 调用 `chatMemory.clear(sessionId)` 真清空 |
| `ChatController`（修改） | 会话 ID 按 userId 隔离（`{userId}:{clientSessionId}`），Redis key 可识别归属 |
| `ChatPanel.vue`（修改） | sessionId 持久化到 sessionStorage（误刷新不丢、关页重进为新会话） |
| `mrb-meeting-service/pom.xml` | 新增 `spring-boot-starter-test`（test scope） |

## 业务影响范围

- AI 助手会话链路（mrb-meeting-service），前端 ChatPanel。
- 无 DB 变更、无 MQ 变更、无新增第三方服务。

## 冲突与风险

- **Redis 故障时的行为**：会话记忆读写失败按"降级"处理（记录日志、不阻断对话），
  避免 Redis 抖动导致整个聊天不可用；代价是故障期间会话上下文不连续。
- **消息元数据**：与官方 JDBC 实现一致，仅持久化消息文本与类型，
  AssistantMessage 的工具调用元数据与 ToolResponse 内容不持久化（同轮上下文不受影响）。
- **安全边界**：会话按 userId 隔离，跨用户无法复用/猜测会话 ID 读取他人上下文；
  后端在会话 ID 前拼接 userId，前端无感知。
