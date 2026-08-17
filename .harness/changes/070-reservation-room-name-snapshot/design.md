# 070 技术方案：预约会议室名称快照

## 1. 背景与目标

当前预约展示链路通过 `LEFT JOIN meeting_room` 或 `selectById` 实时获取会议室名称。由于
`meeting_room` 使用逻辑删除，会议室删除后这些查询会自动过滤 `deleted=0`，导致历史预约的会议室列显示空白。

目标是在预约创建时固化会议室名称，使预约历史展示不依赖会议室记录是否仍存在。

## 2. 方案选型

| 方案 | 说明 | 结论 |
|------|------|------|
| 预约表冗余 `room_name` 快照 | 创建时写入名称，展示直接读取；会议室删除/改名不影响历史 | 采用 |
| 查询时忽略会议室逻辑删除 | 展示最后名称，但需逐处改查询且保留不了“创建时名称” | 不采用 |
| 禁止删除有历史预约的会议室 | 产品约束过强，影响会议室清理 | 不采用 |

## 3. 数据模型

```sql
meeting_room_reservation.room_name VARCHAR(64) DEFAULT NULL
```

迁移脚本同时回填历史数据：

```sql
UPDATE meeting_room_reservation r
LEFT JOIN meeting_room mr ON mr.id = r.room_id
SET r.room_name = mr.name
WHERE r.room_name IS NULL
  AND mr.name IS NOT NULL;
```

该回填 SQL 不受 MyBatis-Plus 逻辑删除影响，即使会议室已删除仍可回填名称。

## 4. 写入与读取路径

### 4.1 写入

创建预约：

```text
MeetingRoom room = selectByIdForUpdate(roomId)
reservation.roomName = room.name
save(reservation)
```

### 4.2 读取

统一解析规则：

```text
if reservation.roomName is present -> use snapshot
else if roomNameMap contains roomId -> use map value
else -> empty string
```

该规则用于：

- `ReservationVO` 转换；
- `ScheduleReservationVO` 转换；
- `ReservationToolResults.toBrief`；
- 会议提醒通知。

列表 XML 查询直接返回 `r.room_name`，不再 JOIN `meeting_room`。

## 5. 兼容性

- 新记录写入快照，旧记录由迁移回填。
- 若迁移前存在 `room_name` 为空的边界数据，读取端回退到外部名称映射或空字符串。
- `room_name` 为冗余字段，不影响现有预约创建接口契约。

## 6. 测试策略

- 创建预约断言保存 `roomName`。
- 会议室逻辑删除后详情查询断言仍返回快照。
- 完整服务模块测试验证 XML/服务改动无回归。
