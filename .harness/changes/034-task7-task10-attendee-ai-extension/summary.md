# task7 + task10：参会人邀请 & AI 助手一期扩展

## 需求描述

- **task7**：实现会议室预约参会人邀请功能，AI 助手可在预约完成后按部门邀请参会人
- **task10（一期）**：扩展 AI 助手能力，新增会议室推荐、空闲时段查询、历史预约查询

## 技术变更清单

### 后端

**mrb-user**
- `UserController`：新增 `/user/internal/list-by-department`、`/user/internal/list-by-ids` 两个内部接口供跨服务调用
- `UserService` + `UserServiceImpl`：新增 `listByIdsDetailed(Collection<Long>)` 方法，按 ID 批量返回带部门信息的 UserVO

**mrb-meeting**
- 新增 `feign/dto/UserBriefDTO`：跨服务传输的用户简要信息 DTO
- 新增 `feign/dto/DepartmentBriefDTO`：跨服务传输的部门信息 DTO
- 新增 `feign/DepartmentFeignClient`：调用 mrb-user 的 `/department/list` 接口
- `UserFeignClient`：新增 `listByDepartment(departmentId)` 和 `listByIds(ids)` 两个 Feign 方法
- 新增 `controller/ReservationAttendeeController`：暴露参会人邀请/查询/移除 HTTP 接口
- `service/ReservationAttendeeService` + 新增 `impl/ReservationAttendeeServiceImpl`：参会人业务逻辑实现
  - `inviteAttendees`：按用户ID列表追加邀请（已存在跳过、邀请人自身不重复加入）
  - `inviteDepartment`：通过 Feign 查部门成员后追加邀请
  - `listAttendees`：批量 Feign 回填用户信息
  - `removeAttendee`：校验预约归属后删除

**mrb-common**
- `ErrorCode`：新增 `ATTENDEE_NOT_INVITED(1021)`、`ATTENDEE_ALREADY_INVITED(1022)`、`RESERVATION_ACCESS_DENIED(1023)`

**AI 工具扩展** `tools/MeetingRoomTool`
- 新增 `recommendRoom`：按日期/时段/人数/设备推荐空闲会议室
- 新增 `findFreeSlots`：计算某会议室某日的空闲时段
- 新增 `listMyReservationHistory`：查询本人历史预约（含已完成/已取消），返回统计 + 前 10 条列表
- 新增 `listDepartments`：通过 DepartmentFeignClient 查询部门列表
- 新增 `inviteDepartmentAttendees`：按部门邀请参会人
- 新增 `listReservationAttendees`：查询某预约的参会人列表

**系统提示词** `prompt/chatbot-system-prompt.md`
- 新增功能清单条目
- 新增 4 个典型场景引导（推荐会议室、邀请参会人、空闲时段、历史查询）
- 强化越权防护说明

### 数据库
- 复用 task7 已建表 `reservation_attendee`（V1.10__add_reservation_attendee_table.sql）

## 接口设计

| Method | Path | 说明 |
|--------|------|------|
| POST | `/meeting/reservation/attendee/{reservationId}/invite` | 按用户ID列表邀请参会人 |
| POST | `/meeting/reservation/attendee/{reservationId}/invite-department` | 按部门邀请参会人 |
| GET  | `/meeting/reservation/attendee/{reservationId}/list` | 查询参会人列表 |
| DELETE | `/meeting/reservation/attendee/{reservationId}/{userId}` | 移除参会人 |
| GET | `/user/internal/list-by-department?departmentId=` | 内部：按部门查用户 |
| GET | `/user/internal/list-by-ids?ids=` | 内部：按ID批量查用户 |

## 冲突与风险

- **风险1**：AI 工具增多到 11 个，LLM 误选工具的概率上升，已通过清晰的工具描述与场景引导降低风险
- **风险2**：邀请参会人需严格校验预约归属，已在 Service 层通过 `checkReservationOwnership` 实现
- **风险3**：跨服务 Feign 调用失败时通过降级（返回空集合/提示重试）保证核心功能可用
- **风险4**：参会人邀请为追加式（不覆盖已邀请的），避免误删历史参会人

## 完成校验

- [x] 后端代码编译通过（mvn compile）
- [x] 红线零违规：价格字段无涉及、Redis Key 前缀无涉及、RocketMQ 无涉及、异常走 BusinessException、Controller 构造器注入、@Transactional 声明 rollbackFor、API 响应遵循 Result 包装
- [x] 分层正确：Controller → Service → Repository → Model，跨服务通过 Feign
- [x] 系统提示词与新工具同步更新
