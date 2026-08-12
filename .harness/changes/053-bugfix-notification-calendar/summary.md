# 053 Bugfix - 预约创建重复通知 + 日历视图无数据块

> 修复两个线上问题：1) 创建免审批预约时参会人收到两条相同通知；2) 所有日历视图不再显示预约数据块。

## 需求描述与验收标准

1. 创建免审批预约（状态直接为已确认）时，每位参会人只收到一条 `RESERVATION_CREATED` 通知。
2. 日程视图（我的日历 / 会议室日历）与会议室详情页预约日历恢复显示预约数据块，日/周/月视图均正常。

验收标准：
- 后端编译通过，前端 `npm run build`（vue-tsc + vite）通过。
- 日历日期过滤与时间定位兼容后端当前 `yyyy-MM-dd HH:mm:ss`（空格）格式。

## 背景与根因

### bug1 重复通知
- `ReservationServiceImpl.createReservation` 第 8 步调用 `inviteAttendees`，其内部在预约状态为已确认时已发送一次 `RESERVATION_CREATED` 通知；
- 第 9 步又对相同参会人重复发送一次，导致每位参会人收到两条相同通知（id 不同、内容与时间相同）。

### bug2 日历视图无数据块
- 日历 v2（`ScheduleViewV2` / `RoomScheduleView`）于 7/29 基于后端 ISO `yyyy-MM-dd'T'HH:mm:ss` 格式编写，使用 `startTime.split('T')[0]` 提取日期；
- 8/1 提交 `5040f7f` 将后端 Jackson 全局时间格式统一为空格分隔 `yyyy-MM-dd HH:mm:ss`，日历视图未同步适配；
- 空格格式下 `split('T')[0]` 返回完整时间串，永远不等于 `yyyy-MM-dd` 日期，导致日/周/月视图过滤结果全为空；
- 此外 `new Date("2026-08-12 16:00:00")` 在部分浏览器（如 Safari）解析为 Invalid Date，事件块位置计算失效。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `ReservationServiceImpl.java` | 删除 `createReservation` 第 9 步重复通知，通知统一由 `inviteAttendees` 内部发送 |
| `frontend/src/utils/datetime.ts` | 新增 `toDate()`：兼容空格/ISO 两种分隔符解析为 `Date` |
| `frontend/src/views/schedule/ScheduleViewV2.vue` | 日期提取改用 `formatDateStr()`（兼容两种格式）；时间解析改用 `toDate()` |
| `frontend/src/components/RoomScheduleView.vue` | 同上 |
| `.harness/changes/053-bugfix-notification-calendar/summary.md` | 本次变更追踪 |

## 方案说明

- 通知去重选择保留 `inviteAttendees` 内的发送逻辑：其对“新增参会人”发送，天然覆盖增量邀请（含按部门邀请），且审批流（PENDING → approveReservation 后通知）不受影响。
- 日历视图不再直接 `split('T')`，统一走 `formatDateStr`（`replace('T',' ')` + 取前 10 位），对空格与 ISO 两种历史格式均兼容；`toDate` 将空格归一化为 ISO 后再 `new Date`，规避 Safari 解析问题。

## 冲突与风险

- 无后端接口变更，前端为展示层兼容修复。
- 已废弃未引用的 `RoomCalendar.vue` / `TimeSlotCalendar.vue` 不在本次修改范围（YAGNI），后续清理时再统一。
