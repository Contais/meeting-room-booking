# 预约改为通讯录选参会人 + AI助手闭环流程

## 需求描述

将预约会议室时的「手机号 + 人数」字段去掉，改为从通讯录选择参会人员。同时需要重新设计 AI 助手自动预约的交互流程，确保闭环。

## 现状分析

### 当前预约表单字段（BookingDialog.vue）
- 会议主题 subject
- 预约日期 / 时间段
- **参会人数 attendeeCount**（待移除）
- **联系电话 contactPhone**（待移除）
- 备注 remark

### 当前数据模型（MeetingRoomReservation）
- `attendee_count`：参会人数（冗余字段，可直接移除或保留为派生值）
- `contact_phone`：联系人手机号（待移除）
- 无参会人明细表

### 当前 AI 助手预约流程（MeetingRoomTool.createReservation）
- AI 通过 ToolContext 获取 userId
- 参数：roomName / date / startTime / endTime / subject / attendeeCount（可选）
- 直接调用 `reservationService.createReservation(userId, dto)` 完成预约
- **无参会人选择环节**，AI 无法替用户选参会人

## 核心矛盾

| 场景 | 矛盾点 |
|------|--------|
| 手动预约 | 改为通讯录选人后，表单交互合理，无矛盾 |
| AI 助手预约 | AI 无法替用户决定"邀请谁"，强行让 AI 选人会越权且不自然 |
| AI 助手预约 | 参会人可能需要对方确认（是否参会），AI 单向创建无法闭环 |

## 方案设计

### 1. 数据模型变更

**新增表 `reservation_attendee`（预约参会人）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| reservation_id | bigint | 预约ID（FK → meeting_room_reservation.id） |
| user_id | bigint | 参会人用户ID |
| attend_status | tinyint | 参会状态: 0-待回复, 1-已接受, 2-已拒绝, 3-待定 |
| create_time | datetime | 创建时间 |

**修改 `meeting_room_reservation` 表**
- 移除 `contact_phone` 字段
- `attendee_count` 改为派生值（由参会人表 count 得出），或保留字段由系统自动填充
- 新增 `organizer_id`（组织者ID，即预约人，复用现有 user_id）

### 2. 前端表单改造

- 移除「参会人数」「联系电话」输入框
- 新增「参会人员」选择器：从通讯录（/user/contacts）多选参会人
- 参会人列表展示：头像 + 姓名 + 部门
- 参会人数自动统计，无需手动输入

### 3. AI 助手闭环流程设计

**方案 A：AI 仅创建预约骨架，参会人由用户手动补全（推荐）**

```
用户: "帮我预约明天下午2-3点第一会议室，讨论需求"
AI:   → createReservation(roomName, date, time, subject)
      → 返回预约编号 + 提示"已创建预约，请前往详情页添加参会人员"
用户: (在详情页/预约页手动选参会人)
```

优点：AI 流程简洁，不越权替用户选人；参会人选择权归用户
缺点：多一步手动操作

**方案 B：AI 创建预约后，引导用户在对话中选参会人**

```
用户: "帮我预约明天下午2-3点第一会议室，讨论需求"
AI:   → createReservation(roomName, date, time, subject)
      → "已创建预约。需要邀请参会人吗？我可以列出你的常用联系人"
用户: "邀请张三、李四"
AI:   → addAttendees(reservationId, [张三ID, 李四ID])
      → "已邀请张三、李四"
```

优点：闭环在对话内完成
缺点：AI 工具需新增 `addAttendees` / `searchContacts` 工具，对话轮次增加

**方案 C：AI 创建预约 + 自动邀请常用联系人（基于历史）**

不推荐——越权风险高，可能邀请错误的人。

### 4. 参会人通知与确认闭环

- 预约创建后，系统向参会人发送站内信通知（依赖 task8 站内信）
- 参会人可在「我的会议」页面接受/拒绝邀请
- 组织者可在预约详情查看参会状态
- **未实现站内信前**：参会人状态默认「待回复」，仅记录关系

## 技术变更清单

| 类型 | 文件 | 说明 |
|------|------|------|
| 新增 | DB: `reservation_attendee` 表 | 参会人明细 |
| 修改 | `MeetingRoomReservation.java` | 移除 contactPhone，attendeeCount 改派生 |
| 新增 | `ReservationAttendee.java` | 参会人实体 |
| 新增 | `ReservationAttendeeRepository.java` | 参会人数据访问 |
| 修改 | `ReservationCreateDTO.java` | 移除 contactPhone/attendeeCount，新增 attendeeUserIds |
| 修改 | `ReservationServiceImpl.java` | createReservation 批量写入参会人 |
| 修改 | `MeetingRoomTool.java` | AI 工具新增 addAttendees（方案B） |
| 修改 | `BookingDialog.vue` | 移除手机号/人数，新增通讯录多选参会人 |
| 修改 | `ReservationDetail.vue` | 展示参会人列表及状态 |

## 业务影响范围

- 预约创建流程（手动 + AI）
- 预约详情展示
- 通讯录模块（作为数据源）
- 站内信模块（通知参会人，依赖 task8）

## 冲突与风险

- **风险1**：AI 助手原有 `attendeeCount` 参数需废弃或改为可选，需同步更新 Tool 描述
- **风险2**：参会人通知依赖站内信（task8），若站内信未就绪，参会人确认流程无法闭环
- **风险3**：通讯录多选组件在 el-drawer 内的交互体验需验证（滚动、搜索）
- **风险4**：历史数据的 `contact_phone` / `attendee_count` 字段需做数据迁移策略（保留/清空）

## 依赖

- task8 站内信（用于参会人通知，非强依赖，可分期）
- 通讯录 API 已就绪（/user/contacts）
