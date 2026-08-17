# 069 会议室预约完整性与逻辑漏洞修复

## 需求描述与验收标准

### 预约创建链路

1. 禁止创建开始时间早于或等于当前时间的预约，`startTime` 必须晚于 `now`。
2. 冲突检测必须原子化：同一会议室的“查询冲突 -> 插入预约”不得因并发而同时通过检查。
3. 会议室规则值必须在写入和预约读取两个链路防御性校验：
   - `bookableStart` / `bookableEnd` 必须为合法 `HH:mm`；
   - `bookableStart` 必须早于 `bookableEnd`；
   - `maxDuration` 必须大于 0；
   - `minDuration` / `advanceDays` 不得为负数；
   - 非法值返回业务错误（`BusinessException` / `PARAM_ERROR`），不得因 `LocalTime.parse` 抛异常返回 500。
4. 实现“单次最小预约时长”：
   - `meeting_room` 新增 `min_duration`；
   - 创建/更新会议室可配置；
   - 创建预约时校验预约时长不低于该值。
5. 联系人手机号不采集：维持现状，不新增 DTO/实体映射字段。

### 会议室管理链路

1. 删除会议室前校验是否存在未结束预约（状态非“已取消/已拒绝”且 `end_time > now`），存在则拒绝删除。
2. 删除操作与预约创建共用会议室行锁，避免删除与创建并发竞态产生孤儿预约。

## 技术变更清单

### 数据库

- `backend/sql/init.sql`
  - `meeting_room` 增加 `min_duration INT DEFAULT 0 COMMENT '单次最小预约时长(分钟)，0表示不限制'`。
  - 种子 INSERT 同步包含 `min_duration` 列（默认 0，不改变现有行为）。
- 新增 `backend/sql/V1.22__add_room_min_duration.sql`
  - 已部署环境增量添加 `meeting_room.min_duration`。

### 后端模型

- `MeetingRoom` 增加 `minDuration`。
- `RoomCreateDTO` / `RoomUpdateDTO` 增加 `minDuration`。
- `MeetingRoomVO` 增加 `minDuration`。

### 后端数据访问

- `MeetingRoomRepository` 新增 `selectByIdForUpdate(Long id)`：
  - `SELECT * FROM meeting_room WHERE id = #{id} AND deleted = 0 FOR UPDATE`。
  - 用于会议室行级锁，串行化同一会议室的预约创建和删除。

### 后端服务

- 新增 `MeetingRoomRuleValidator`：
  - 统一校验时间窗口、最小/最大时长、提前预约天数。
- `ReservationServiceImpl`
  - 创建预约改用 `selectByIdForUpdate` 获取会议室并持锁到事务提交。
  - 新增 `startTime > now` 校验。
  - `validateRoomRules` 先做防御性规则校验，再解析时间窗口。
  - 新增最小预约时长校验。
  - 可预约时间解析使用固定 `HH:mm` 格式并捕获 `DateTimeParseException`，兜底返回业务错误。
- `MeetingRoomServiceImpl`
  - `createRoom` 补齐 `minDuration` 默认值并在保存前校验规则。
  - `updateRoom` 在复制非空字段后校验合并后的完整规则。
  - `deleteRoom` 使用会议室行锁，并检查未结束预约后执行逻辑删除。
  - `toVO` 返回 `minDuration`。

### 测试

- `ReservationServiceImplTest`
  - 新增过去时间、非法存储时间、低于最小预约时长测试。
  - 现有用例改为验证 `selectByIdForUpdate`。
- `MeetingRoomServiceImplTest`
  - 新增默认/自定义 `minDuration`、非法时间、负数时长、最小>最大、删除有/无活动预约测试。

## 业务影响范围

- 会议室创建/编辑：新增可配置“单次最小预约时长”，默认 0（不限制）。
- 预约创建：对过去时间、非法规则值、低于最小预约时长、并发冲突有更严格防护。
- 会议室删除：有未结束预约时不再允许直接删除。
- 历史脏数据：预约创建读取到非法 `bookableStart`/`bookableEnd` 时由业务异常兜底，不再 500。

## 冲突与风险

- `min_duration` 默认 0，避免对现有会议室预约行为产生破坏性变化；需要明确各会议室的最小预约时长时，由管理员在会议室规则中配置。
- 会议室行锁仅作用于单会议室，不会影响不同会议室之间的并发。
- `selectByIdForUpdate` 要求事务内使用；创建预约和删除会议室方法均已声明 `@Transactional(rollbackFor = Exception.class)`。
- 删除会议室仅拦截“未结束”预约，历史已结束预约仍保留，并依赖 LEFT JOIN 在查询时兼容已删除会议室名称。

## 验证结果

- `mvn -pl :mrb-meeting-service -am test`
  - 测试：48 个，失败：0，错误：0。
- 红线自检：
  - 价格字段：不涉及。
  - Redis Key 前缀 `mrb:`：不涉及新增 Key。
  - MQ 幂等：不涉及。
  - 异常体系：全部使用 `BusinessException`。
  - Controller 构造器注入：不涉及 Controller。
  - `@Transactional`：写操作均声明 `rollbackFor = Exception.class`。
  - Vue `<script setup>`：不涉及前端。
  - 响应 `{code, message, data}`：不涉及 Controller。
