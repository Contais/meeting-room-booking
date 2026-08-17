# 069 技术方案：预约创建原子性与会议室规则完整性

## 1. 背景

当前 `createReservation` 采用“先 `checkTimeConflict`，后 `save`”的非原子流程，并发请求可能同时通过冲突检查并写入同一时段。会议室规则仅依赖数据库默认值和创建时人工填写，预约读取时直接 `LocalTime.parse`，非法配置会以 `DateTimeParseException` 形式返回 500。同时业务模型中的“单次最小预约时长”缺少数据字段与实现。

## 2. 方案选型

### 2.1 预约冲突原子化

候选方案：

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| 会议室行级锁 `SELECT ... FOR UPDATE` | 依赖数据库既有事务机制，无需额外组件；锁粒度为单会议室，实现简单 | 需要新增 Repository 方法 | 采用 |
| Redis 分布式锁 | 多实例可用 | 需处理锁超时、误删、持有者标识，复杂度更高 | 不采用 |
| 数据库唯一约束 | 数据库层面强制 | 无法用普通唯一索引表达时间区间重叠，需触发器/复杂设计 | 不采用 |

实现：

- `MeetingRoomRepository.selectByIdForUpdate(id)` 在事务内锁定 `meeting_room` 行。
- `createReservation` 和 `deleteRoom` 都使用该方法：
  - 创建预约：持锁 -> 规则校验 -> 冲突查询 -> 插入，事务提交后释放。
  - 删除会议室：持锁 -> 未结束预约查询 -> 逻辑删除，事务提交后释放。
- 锁竞争只影响同一会议室，不影响其他会议室。

### 2.2 规则校验分层

新增 `MeetingRoomRuleValidator`，统一规则语义：

1. 时间窗口：
   - 非空；
   - 格式严格 `HH:mm`；
   - `bookableStart < bookableEnd`。
2. 时长：
   - `minDuration >= 0`；
   - `maxDuration > 0`；
   - `minDuration <= maxDuration`。
3. 提前预约天数：
   - `advanceDays >= 0`。

调用点：

- 写入链路 `createRoom` / `updateRoom`：在保存前校验，阻止脏数据入库。
- 读取链路 `createReservation`：对历史脏数据做防御性校验，避免 500。

## 3. 数据模型变更

`meeting_room` 新增：

```sql
min_duration INT DEFAULT 0 COMMENT '单次最小预约时长(分钟)，0表示不限制'
```

默认值 0 保持向后兼容，不强制现有会议室设置最小预约时长。

## 4. 关键代码路径

### 4.1 创建预约

```text
MeetingRoomRepository.selectByIdForUpdate(roomId)
  -> 校验会议室存在/启用
  -> 校验 startTime > now
  -> 校验 endTime > startTime
  -> MeetingRoomRuleValidator.validate(room rules)
  -> 校验提前预约天数 / 可预约时段 / 最小时长 / 最大时长
  -> 查询冲突（此时同一会议室的并发创建已串行化）
  -> 保存预约
  -> 生成预约编号 / 参会人 / 通知
```

### 4.2 删除会议室

```text
MeetingRoomRepository.selectByIdForUpdate(id)
  -> 校验会议室存在
  -> 查询未结束预约：
       status NOT IN (CANCELLED, REJECTED)
       AND end_time > now
  -> 存在则 BusinessException
  -> meetingRoomRepository.deleteById(id)
```

## 5. 测试策略

- 单元测试覆盖：
  - 过去时间被拒绝；
  - 非法存储时间返回 `PARAM_ERROR` 而非解析异常；
  - 低于 `minDuration` 被拒绝；
  - 创建/更新时非法规则不落库；
  - 删除会议室时存在未结束预约被拒绝；
  - 无未结束预约可正常逻辑删除。
- 并发锁本身依赖 MySQL InnoDB 行锁，由事务语义保证；单元测试验证服务使用锁方法。

## 6. 兼容性与回滚

- 数据库迁移只新增字段，可安全执行。
- 应用回滚后 `min_duration` 字段仍存在但不被读取，不影响旧版本运行。
- `min_duration` 默认 0，不改变既有预约行为。
