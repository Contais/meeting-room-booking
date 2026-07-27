# 站内信 + 工作流公共组件（需求分析）

## 需求描述

1. **站内信**：实现系统内的消息通知机制，覆盖预约通知、审批通知、系统公告等场景
2. **工作流**：实现审批工作流，前端需封装为公共组件（审批日志、审批操作），便于复用到各业务模块

## 一、站内信

### 1.1 业务场景

| 场景 | 触发时机 | 接收人 |
|------|----------|--------|
| 预约创建通知 | 用户创建预约 | 预约人 + 参会人（task7） |
| 预约审批通知 | 会议室需审批时 | 管理员（待审批）/ 预约人（审批结果） |
| 预约取消通知 | 预约被取消 | 参会人 |
| 预约即将开始 | 会议开始前 15/30 分钟 | 预约人 + 参会人 |
| 账号状态变更 | 管理员启用/禁用账号 | 被操作用户 |
| 系统公告 | 管理员发布 | 全体/指定角色 |

### 1.2 数据模型设计

**`notification`（站内信表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| user_id | bigint | 接收人ID |
| type | varchar(32) | 类型: RESERVATION/APPROVAL/SYSTEM |
| title | varchar(128) | 标题 |
| content | text | 内容 |
| ref_type | varchar(32) | 关联业务类型（如 reservation） |
| ref_id | bigint | 关联业务ID |
| is_read | tinyint | 已读: 0-未读, 1-已读 |
| create_time | datetime | 创建时间 |

**`notification_read`（已读记录表，可选）**

若需记录阅读时间，可拆出独立表。当前简化为 `notification.is_read`。

### 1.3 接口设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/notification/page` | GET | 分页查询当前用户消息 |
| `/api/notification/unread-count` | GET | 未读消息数（用于角标） |
| `/api/notification/read` | POST | 标记消息已读 |
| `/api/notification/read-all` | POST | 全部已读 |
| `/api/notification/delete` | DELETE | 删除消息 |

### 1.4 前端设计

- 顶部导航栏消息图标 + 未读角标
- 消息中心页面（/notifications）：分类 Tab（全部/预约/审批/系统）
- 消息项支持点击跳转关联业务页面（通过 ref_type + ref_id）
- WebSocket 实时推送（可选，一期可用轮询）

### 1.5 后端模块归属

- 归入 `mrb-user` 模块（与用户强相关）
- 提供 `NotificationService`，其他模块通过事件/MQ 发送通知（依赖 task9 MQ）

## 二、工作流公共组件

### 2.1 业务场景

| 场景 | 审批流 |
|------|--------|
| 会议室预约审批 | 需审批会议室 → 管理员审批 |
| （未来）请假审批 | 员工提交 → 主管审批 |
| （未来）设备借用 | 员工申请 → 管理员审批 |

### 2.2 数据模型设计

**`workflow_instance`（工作流实例）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| biz_type | varchar(32) | 业务类型: RESERVATION 等 |
| biz_id | bigint | 业务ID |
| initiator_id | bigint | 发起人ID |
| status | tinyint | 状态: 0-审批中, 1-通过, 2-拒绝, 3-撤回 |
| create_time | datetime | 创建时间 |
| finish_time | datetime | 完成时间 |

**`workflow_node`（审批节点，可选，一期可简化为单节点）**

一期简化：单级审批，无需多节点配置表。

**`workflow_log`（审批日志）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| instance_id | bigint | 工作流实例ID |
| operator_id | bigint | 操作人ID |
| action | varchar(16) | 操作: SUBMIT/APPROVE/REJECT/CANCEL |
| comment | varchar(500) | 审批意见 |
| create_time | datetime | 操作时间 |

### 2.3 前端公共组件设计

**`<ApprovalPanel>` 审批操作面板组件**

```vue
<ApprovalPanel
  :biz-type="'RESERVATION'"
  :biz-id="reservationId"
  :current-user-id="userId"
  @approved="onApproved"
  @rejected="onRejected"
/>
```

Props:
- `bizType`：业务类型
- `bizId`：业务ID
- `currentUserId`：当前用户（判断是否可审批）

功能：
- 展示当前审批状态
- 审批人可输入审批意见、通过/拒绝
- 普通用户可撤回

**`<ApprovalLog>` 审批日志组件**

```vue
<ApprovalLog :instance-id="instanceId" />
```

功能：
- 时间线展示审批流程
- 每个节点：操作人头像、操作类型、意见、时间
- 复用于预约详情、未来其他审批页面

### 2.4 接口设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workflow/instance` | GET | 查询业务关联的工作流实例 |
| `/api/workflow/approve` | POST | 通过审批 |
| `/api/workflow/reject` | POST | 拒绝审批 |
| `/api/workflow/cancel` | POST | 撤回审批 |
| `/api/workflow/logs` | GET | 审批日志列表 |

### 2.5 与现有预约审批的整合

当前预约审批逻辑散落在 `ReservationServiceImpl`（approve/reject 方法）。
改造方向：
1. 将审批逻辑抽象为通用 `WorkflowService`
2. 预约审批调用 `WorkflowService.submit/approve/reject`
3. 预约详情页引入 `<ApprovalPanel>` + `<ApprovalLog>` 组件

### 2.6 后端模块归属

- 新建 `mrb-workflow` 模块？**不推荐**（过度拆分）
- 归入 `mrb-meeting`（一期仅会议室审批用）或 `mrb-user`（通用服务）
- **建议**：归入 `mrb-user`（通用能力），`mrb-meeting` 通过 Feign/直调依赖

## 技术变更清单

| 类型 | 文件/模块 | 说明 |
|------|-----------|------|
| 新增 | DB: `notification` 表 | 站内信 |
| 新增 | DB: `workflow_instance` / `workflow_log` 表 | 工作流 |
| 新增 | `mrb-user/.../notification/` 包 | 站内信模块 |
| 新增 | `mrb-user/.../workflow/` 包 | 工作流模块 |
| 新增 | `frontend/.../components/ApprovalPanel.vue` | 审批操作组件 |
| 新增 | `frontend/.../components/ApprovalLog.vue` | 审批日志组件 |
| 新增 | `frontend/.../views/notification/` | 消息中心页面 |
| 修改 | `MainLayout.vue` | 顶部消息图标 + 未读角标 |
| 修改 | `ReservationDetail.vue` | 引入审批组件 |

## 业务影响范围

- 所有需要通知用户的场景
- 所有需要审批的业务（一期：会议室预约）

## 冲突与风险

- **风险1**：站内信实时推送依赖 WebSocket 或轮询，需评估技术方案
- **风险2**：工作流通用化后，现有预约审批逻辑需重构，需保证兼容
- **风险3**：站内信发送依赖 MQ（task9），若 MQ 未就绪需用同步发送兜底
- **风险4**：审批组件需兼顾「单级审批」（一期）与未来「多级审批」扩展性

## 依赖

- task9 MQ（站内信异步发送，非强依赖，可同步发送）
- task7 参会人（通知参会人，非强依赖）
