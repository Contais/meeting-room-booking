# 已有功能 AI 助手接管分析

## 需求描述

分析现有系统功能，识别哪些功能可以交给 AI 助手完成，降低用户操作成本。

## 现有 AI 助手能力

当前 AI 助手（MeetingRoomTool）已支持：

| 工具 | 功能 |
|------|------|
| `listAvailableRooms` | 查询可用会议室 |
| `queryRoomReservationsNew` | 查询会议室某日预约情况 |
| `todayReservationStats` | 今日预约统计 |
| `createReservation` | 创建预约 |
| `cancelMyReservation` | 取消本人预约 |
| `listMyUpcomingReservations` | 查看本人未结束预约 |

## 已有功能盘点与 AI 接管评估

### 第一梯队：高价值、低风险（建议优先接入）

| 功能 | 当前操作路径 | AI 接管方式 | 价值 | 风险 |
|------|-------------|-------------|------|------|
| **查询我的预约历史** | 我的预约 → 列表筛选 | 新增 `listMyReservationHistory` 工具 | 高 | 低（只读） |
| **会议室推荐** | 手动浏览列表对比 | 新增 `recommendRoom` 工具，按人数/设备/时段推荐 | 高 | 低（只读） |
| **查询空闲时段** | 日历视图肉眼查找 | 新增 `findFreeSlots` 工具，给定日期+会议室返回空闲段 | 高 | 低（只读） |
| **修改预约** | 详情页编辑 | 新增 `updateReservation` 工具 | 中 | 中（需权限校验） |

### 第二梯队：中价值、需设计（建议二期）

| 功能 | 当前操作路径 | AI 接管方式 | 价值 | 风险 |
|------|-------------|-------------|------|------|
| **邀请参会人** | 预约后手动添加 | 新增 `addAttendees` 工具（依赖 task7） | 中 | 中（越权风险） |
| **审批预约** | 管理后台审批 | 新增 `approveReservation` / `rejectReservation` 工具 | 中 | 高（权限敏感） |
| **查询部门通讯录** | 通讯录页面浏览 | 新增 `searchContacts` 工具 | 中 | 低（只读） |
| **会议提醒设置** | 无（未来功能） | AI 对话设置提醒 | 低 | 低 |

### 第三梯队：低价值或不适合 AI（不建议）

| 功能 | 原因 |
|------|------|
| 用户管理（增删改） | 管理员操作，AI 接管风险高 |
| 会议室管理（增删改） | 管理员操作，低频，AI 无优势 |
| 角色权限管理 | 安全敏感，不适合 AI |
| 菜单/部门管理 | 配置类操作，AI 无优势 |
| 个人资料修改 | 简单表单，AI 无优势 |
| 密码修改 | 安全敏感，不适合 AI |

## 优先接入方案详述

### 1. 会议室推荐（推荐优先做）

**用户场景**：
```
用户: "明天下午要开个 10 人的会，需要投影仪"
AI:   → recommendRoom(date, capacity, equipment)
      → "推荐以下会议室：1. 第一会议室（3楼A301，12人，投影仪）2. ..."
```

**工具定义**：
```java
@Tool(description = "根据需求推荐可用会议室。传入日期、时段、人数、设备需求")
public String recommendRoom(
    @ToolParam(description = "日期 yyyy-MM-dd") String date,
    @ToolParam(description = "开始时间 HH:mm", required = false) String startTime,
    @ToolParam(description = "结束时间 HH:mm", required = false) String endTime,
    @ToolParam(description = "参会人数", required = false) Integer capacity,
    @ToolParam(description = "设备需求，如 投影仪/白板", required = false) String equipment
)
```

### 2. 查询空闲时段

**用户场景**：
```
用户: "第一会议室明天哪些时段空着？"
AI:   → findFreeSlots(roomName, date)
      → "第一会议室明天空闲时段：09:00-11:00, 14:00-18:00"
```

### 3. 修改预约

**用户场景**：
```
用户: "把我明天下午的会议改到 3 点开始"
AI:   → listMyUpcomingReservations() → 定位预约
      → updateReservation(reservationId, newStartTime)
      → "已修改，新时段 15:00-16:00"
```

### 4. 查询预约历史

**用户场景**：
```
用户: "我上个月开了多少次会？"
AI:   → listMyReservationHistory(startDate, endDate)
      → "您上月共有 12 次预约，其中 10 次已完成、2 次取消"
```

## AI 助手能力扩展后的完整矩阵

| 领域 | 查询 | 操作 |
|------|------|------|
| 会议室 | ✅ 查询列表、统计、推荐、空闲时段 | ❌ 增删改（管理员） |
| 预约 | ✅ 查询本人历史/未来、统计 | ✅ 创建、取消、修改 |
| 参会人 | ✅ 查询通讯录 | ✅ 邀请（依赖 task7） |
| 审批 | ✅ 查询待审批列表 | ⚠️ 审批（权限敏感，二期） |
| 用户/部门 | ✅ 查询通讯录 | ❌ 增删改（管理员） |

## 落地建议

1. **一期（立即）**：会议室推荐 + 空闲时段查询 + 预约历史查询
2. **二期（task7 后）**：修改预约 + 邀请参会人
3. **三期（task8 后）**：审批预约 + 会议提醒

## 冲突与风险

- **风险1**：AI 工具增多后，Tool 描述需清晰，否则 LLM 可能误选工具
- **风险2**：修改/取消预约需严格校验归属，防止越权
- **风险3**：推荐/空闲时段查询需考虑并发（查询时空闲，创建时可能已被占）
- **风险4**：AI 返回数据需脱敏（如手机号），防止信息泄露
