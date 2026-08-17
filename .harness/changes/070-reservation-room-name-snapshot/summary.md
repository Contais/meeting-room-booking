# 070 预约会议室名称快照

## 需求描述与验收标准

会议室被逻辑删除后，历史预约仍需要正确展示“会议室名称”，不能因为
`meeting_room.deleted = 1` 导致预约管理列表、预约详情、日程、提醒、AI 助手等位置显示空白。

验收标准：

1. 创建预约时保存当前会议室名称快照到 `meeting_room_reservation.room_name`。
2. 会议室后续被删除或改名，已创建预约仍展示创建时的会议室名称。
3. 预约管理列表、我的预约、我的会议、我的日历、预约详情、日程视图均读取快照。
4. 会议提醒通知和 AI 助手查询未结束预约时，也能回退读取快照。
5. 已部署环境通过迁移脚本回填历史预约的 `room_name`。

## 技术变更清单

### 数据库

- `backend/sql/init.sql`
  - `meeting_room_reservation` 增加 `room_name VARCHAR(64) DEFAULT NULL COMMENT '会议室名称快照'`。
- 新增 `backend/sql/V1.23__add_reservation_room_name_snapshot.sql`
  - 增加 `room_name` 字段；
  - 从 `meeting_room` 回填历史预约名称，且不受会议室逻辑删除限制。

### 后端模型

- `MeetingRoomReservation` 增加 `roomName` 字段。

### 后端数据访问

- `ReservationRepository.xml`
  - 列表查询由 `mr.name AS room_name` 改为 `r.room_name AS room_name`。
  - 移除仅用于取会议室名称的 `LEFT JOIN meeting_room`，避免已删除会议室导致 `roomName` 为空。

### 后端服务

- `ReservationServiceImpl`
  - 创建预约时写入 `reservation.roomName = room.getName()`。
  - 新增 `roomNameOrDefault`：快照为 null 时返回空字符串，不依赖外部名称映射。
  - `toVO`、`getSchedule`、`getReservationDetail` 直接读取快照。
  - 详情查询不再单独 `selectById` 查询会议室名称。
- `ReservationToolResults`
  - 工具专用预约简要信息在 `roomNameMap` 缺失时回退到 `MeetingRoomReservation.roomName`。
- `ReservationScheduleTask`
  - 提醒通知优先使用预约快照名称，快照缺失时才回查会议室。

### 测试

- `ReservationServiceImplTest`
  - 新增创建预约时保存会议室名称快照的断言。
  - 新增会议室已删除时预约详情仍返回快照名称的测试。

## 业务影响范围

- 预约历史展示不再依赖会议室是否仍存在。
- 会议室改名只影响后续创建的新预约，历史预约保持当时的名称快照。
- AI 助手、日程和提醒通知的会议室名称一致性提升。

## 冲突与风险

- 历史数据必须执行 `V1.23` 迁移回填；否则旧预约可能仍显示空名称。
- `room_name` 是冗余字段，需在创建预约时同步写入；若未来新增其他创建预约入口，也必须写入该字段。
- 名称快照长度与 `meeting_room.name` 保持一致为 64 字符。
- 本方案不改变会议室删除策略，删除会议室仍只拦截未结束预约。

## 验证结果

- `mvn -pl :mrb-meeting-service -am test`
  - 测试：49 个，失败：0，错误：0。
