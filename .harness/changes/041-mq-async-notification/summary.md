# MQ 异步通知 + 定时任务增强

> 对应 031 任务（MQ + 定时任务 + OSS）的 MQ 与定时任务部分。OSS 已由 036/038/039 完成。

## 需求摘要

1. **RocketMQ 接入**：站内信通知由同步 Feign 调用改为 MQ 异步投递，解耦主业务与通知发送。
2. **定时任务增强**：补齐 Redis 分布式锁；新增「会议即将开始提醒」。

## 技术变更清单

### 一、RocketMQ 异步通知链路

| 变更 | 文件 | 说明 |
|------|------|------|
| 新增 | `mrb-common/.../constant/MqConstant.java` | Topic `mrb-notification` / ConsumerGroup 常量 |
| 新增 | `mrb-common/.../model/dto/NotificationMessage.java` | MQ 消息体（msgKey + userIds + template） |
| 修改 | `mrb-common/.../constant/RedisKeyConstant.java` | 新增 MQ_DEDUP / SCHEDULE_LOCK / SCHEDULE_REMINDED |
| 新增 | `mrb-meeting/.../mq/producer/NotificationProducer.java` | 同步发送至 `mrb-notification`，UUID 作 msgKey |
| 新增 | `mrb-platform/.../mq/consumer/NotificationConsumer.java` | 消费 + Redis 幂等去重（24h TTL），失败释放标记等待重试 |
| 修改 | `mrb-meeting/.../service/impl/ReservationServiceImpl.java` | `sendNotificationSafe` 改为 MQ 优先 + Feign 降级 |
| 修改 | `mrb-platform/src/main/resources/application.yml` | 新增 `spring.rocketmq` 配置 |

**消息链路**：`mrb-meeting(Producer) → RocketMQ(mrb-notification) → mrb-platform(Consumer) → NotificationService.sendBatch → DB + WebSocket`

**幂等设计（红线 #3）**：消费端以 `mrb:mq:dedup:mrb-notification:{msgKey}` 做 CAS 去重，处理成功设置 24h TTL，处理失败删除标记允许 MQ 重投递。

**降级策略**：MQ 投递失败 → 降级 Feign 同步调用 → 再失败仅记录日志，不影响主业务。

### 二、定时任务增强

| 变更 | 文件 | 说明 |
|------|------|------|
| 修改 | `mrb-meeting/.../schedule/ReservationScheduleTask.java` | 新增 Redis 分布式锁；新增 `remindUpcomingReservations` |

- **分布式锁**：`mrb:schedule:lock:{task}` + Lua 原子释放（仅持有者删除），TTL 55s 防死锁。
- **超时自动拒绝**（已有，补锁）：每分钟扫描 PENDING 且 start_time < now，CAS 更新为 REJECTED。
- **即将开始提醒**（新增）：每分钟扫描 CONFIRMED 且 startTime ∈ [now, now+15min]，经 MQ 发送站内信；Redis `mrb:schedule:reminded:{reservationId}` 标记防重复（2h TTL，MQ 失败释放标记重试）。

## 冲突与风险

- **MQ 不可用**：Producer 同步发送失败会降级 Feign 同步调用，主业务不受影响。
- **消费幂等**：依赖 Redis 可用；Redis 不可用时去重失效，最坏情况产生重复通知（可接受，非资损）。
- **定时任务时区**：`@Scheduled` 默认 JVM 时区，部署需确认 JVM 时区为 Asia/Shanghai。
- **提醒频率**：每分钟扫描 + 15 分钟窗口 + Redis 标记，每场会议仅提醒一次。

## 手动验证点

1. 启动 RocketMQ（name-server localhost:9876 + broker）。
2. 启动 mrb-meeting(8083) 与 mrb-platform(8084)，观察 Consumer 注册日志。
3. 创建一条免审批预约（通知参会人）→ 确认 `mrb-notification` Topic 有消息、mrb-platform 消费落库、参会人收到站内信 + WebSocket 推送。
4. 审批通过 / 拒绝 / 取消预约 → 确认对应通知经 MQ 投递。
5. 构造一条 startTime 在 15 分钟内的 CONFIRMED 预约 → 等待定时任务触发，确认收到「会议即将开始」提醒且仅一次。
6. 停掉 RocketMQ 后操作预约 → 确认降级 Feign 同步发送，站内信仍可达。
