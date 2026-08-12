# 054 日历视图「我的日历」数据口径变更 - 我预约的 + 我参会的

> 将日程视图「我的日历」Tab 的数据口径从“仅我预约的”扩展为“我预约的 + 我参会的”，
> 并按来源/状态对数据块着色，页面侧栏增加颜色图例。

## 需求描述与验收标准

1. 「我的日历」Tab 显示并集数据：我创建的预约（待确认 / 已确认）+ 我作为参会人的预约（仅已确认）。
2. 数据块颜色可区分来源与状态：我预约·待确认（橙）、我预约·已确认（绿）、我参会·已确认（蓝）。
3. 页面侧栏展示颜色图例，说明各颜色代表的状态。
4. 左侧“今日卡片”同样使用并集口径。

验收标准：
- 后端编译与测试通过，前端 `npm run build`（vue-tsc + vite）通过。
- 新增接口 `/api/meeting/reservation/my-calendar`，不影响既有 `/my`、`/my-meetings` 语义。

## 背景与根因

- 037 设计将「我的日历」Tab 定义为调用 `listMyReservations`（仅我创建的），
  导致被邀请参加的会议不会出现在日历上，用户需额外去「我的会议」页面查询，日历失去日程总览价值。
- 需求评审结论：日历应聚合“我预约的 + 我参会的”，与会同类产品（飞书 / Google Calendar）心智一致。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `ReservationRepository.xml` | 新增 `selectMyCalendarPage`：`user_id = 我 且 status IN (0,1)` UNION 参会人表 EXISTS（status=1） |
| `ReservationRepository.java` | 新增 `selectMyCalendarPage` 方法声明 |
| `ReservationService.java` / `ReservationServiceImpl.java` | 新增 `listMyCalendar`，返回按开始时间升序的预约列表 |
| `ReservationController.java` | 新增 `GET /api/meeting/reservation/my-calendar` |
| `frontend/src/api/reservation.ts` | 新增 `getMyCalendar` |
| `ScheduleViewV2.vue` | 「我的日历」Tab 与今日卡片改用 `getMyCalendar`；新增 `blockClass` 按来源/状态着色；侧栏新增颜色图例；新增 `s-attendee` 蓝色样式 |
| `.harness/wiki/接口协议.md` | 补充 `/my-calendar` 接口说明 |

## 方案说明

- 后端单查询实现并集：我创建的（待确认/已确认）走 `r.user_id = 我`；
  我参会的仅展示已确认（未审批不占用会议室时段），通过 `reservation_attendee` 的 `EXISTS` 子查询关联，
  天然去重（预约人默认也是参会人）。
- 着色规则：前端以 `r.userId === 当前用户ID` 判断来源；我预约的沿用状态色（0 橙 / 1 绿），
  我参会的统一蓝色 `s-attendee`；会议室日历 Tab 不受影响（仅已确认，按状态着色）。
- 时间过滤沿用「完全包含」语义（`start_time >= startTime AND end_time <= endTime`），与既有 `/my` 一致。

## 冲突与风险

- 新增接口不改动既有 `/my`（我的预约）与 `/my-meetings`（我的会议）接口，两个页面行为不变。
- 我参会的预约暂不区分参会人 RSVP 状态（已拒绝也会显示为蓝色块）；如后续需要可按 `ra.status` 过滤。
- 前端「我的日历」与「我的会议」为不同入口，口径差异（日历含待确认、含时间范围）为预期行为。
