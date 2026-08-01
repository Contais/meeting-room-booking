# 通知发送重构 + 邀人页面补全 + ChatPanel 输入法修复

> 解决「AI 助手邀请参会人不更新参会人数/不发送通知」bug 修复过程中暴露的代码异味，
> 并补全页面邀人入口、修复 AI 助手中文输入法误触发 bug。

## 需求摘要

1. **代码异味消除**：`sendNotificationSafe` 在 `ReservationServiceImpl` 与 `ReservationAttendeeServiceImpl` 中 19 行逐字重复；`yyyy-MM-dd HH:mm` 格式 `DateTimeFormatter` 在 3 处定义。
2. **页面邀人补全**：API 与前端封装齐备但页面无邀人入口，AI 助手是唯一邀人路径，存在「特殊入口」风险。
3. **参会人列表排序**：按通知时间 + 真实姓名排序。
4. **ChatPanel 中文输入法 bug**：回车确认候选词会误触发消息发送。

## 技术变更清单

### 一、后端：通知发送重构（消除 DRY 违反）

| 变更 | 文件 | 说明 |
|------|------|------|
| 新增 | `mrb-common/.../constant/DateTimePatternConstant.java` | 集中管理跨模块复用的 `DateTimeFormatter`，提供 `DATETIME_FMT` |
| 新增 | `mrb-meeting/.../mq/producer/NotificationSender.java` | 通知发送外观（Facade）：封装「MQ 优先 + Feign 降级 + 日志兜底」容错策略 |
| 修改 | `mrb-meeting/.../service/impl/ReservationServiceImpl.java` | 删除 `private sendNotificationSafe` 与 `NOTIFY_DATETIME_FMT`；注入 `NotificationSender.sendSafe`；移除 `NotificationFeignClient`/`NotificationProducer` 直依 |
| 修改 | `mrb-meeting/.../service/impl/ReservationAttendeeServiceImpl.java` | 同上 |
| 修改 | `mrb-meeting/.../tools/ReservationTool.java` | `DATE_TIME_FMT` 改为引用 `DateTimePatternConstant.DATETIME_FMT` |

### 二、后端：邀人状态约束 + 参会人列表排序

| 变更 | 文件 | 说明 |
|------|------|------|
| 修改 | `ReservationAttendeeServiceImpl.inviteAttendees` | 加状态校验：仅 `PENDING`/`CONFIRMED` 可邀人；`CONFIRMED` 且 `endTime < now` 拒绝 |
| 修改 | `ReservationAttendeeServiceImpl.listAttendees` | 二次排序：`createTime` 升序，相同时 `realName` 升序（空值居后，大小写不敏感） |
| 修改 | `mrb-meeting/.../model/vo/AttendeeVO.java` | 新增 `createTime` 字段（邀请时间，用于排序与展示） |

### 三、前端：页面邀人 UI 补全

| 变更 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend/src/views/reservation/MyReservationDetail.vue` | 参会人员 section 新增「邀请参会人」按钮 + 弹窗；弹窗支持「按用户」（搜索 + 多选 + 已邀请禁选）/「按部门」（单选）；参会人列表新增「邀请时间」列 |
| 修改 | `frontend/src/types/reservation.d.ts` | `Attendee` 类型补 `createTime?` 字段 |

### 四、前端：ChatPanel 中文输入法 bug 修复

| 变更 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend/src/components/ChatPanel.vue` | `@keyup.enter` 改为 `@keydown.enter`；新增 `compositionstart`/`compositionend` 监听 `isComposing` 状态；回车回调中 composition 期间不发送 |

## 业务流程对齐

邀人时机决策：方案 A（补全页面 + 状态约束）。
- 「审批后不应修改」边界澄清：核心字段（时间/会议室/主题）审批后锁定；参会人列表本质动态，允许增减
- 状态约束：仅 `PENDING`(0) / `CONFIRMED`(1) 可邀人；`CANCELLED`(2) / `REJECTED`(3) / 已结束拒绝
- 通知规则保持现状：免审批立即通知；需审批等 `approveReservation` 时统一通知
- AI 助手与页面走同一 `inviteAttendees` Service，消除「特殊入口」感

## 冲突与风险

- **NotificationSender 与原方法行为一致**：MQ 优先 + Feign 降级 + 日志兜底，逻辑无变化
- **状态校验新增**：原有调用方（创建时同步指定参会人走 `inviteAttendees`）不受影响——创建时预约状态已是 `PENDING` 或 `CONFIRMED`
- **ChatPanel 修复后行为变化**：中文输入法下回车不再发送，需配合 Shift+Enter 或鼠标点击发送按钮（保持原 placeholder 提示「按 Enter 发送」对英文输入法场景仍准确）

## 手动验证点

1. **后端编译**：`mvn -pl mrb-common,mrb-meeting -am compile` 通过
2. **前端类型**：`vue-tsc --noEmit` 通过
3. **通知发送重构回归**：创建免审批预约带参会人 → 参会人收到站内信；审批通过 → 参会人收到邀请通知；取消/拒绝 → 预约人收到通知
4. **邀人状态约束**：
   - 在 CONFIRMED 预约邀人 → 成功
   - 在 CANCELLED/REJECTED 预约邀人 → 报错「预约已取消或已拒绝」
   - 在已结束的 CONFIRMED 预约邀人 → 报错「会议已结束」
5. **页面邀人 UI**：详情页创建者可见「邀请参会人」按钮 → 弹窗按用户/按部门 → 提交后参会人列表刷新
6. **列表排序**：邀请多个参会人（时间相同），按姓名升序排列
7. **ChatPanel 中文输入法**：用中文输入法输入「你好」按回车确认候选词 → 不会发送消息；输入完成后回车 → 才发送
