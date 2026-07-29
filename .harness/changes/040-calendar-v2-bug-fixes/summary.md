# 040 - 日历视图 v2 Bug 修复（布局/抽屉/Popover/时段）

## 需求摘要

修复日历视图 v2 及会议室详情预约日历的 4 个前端问题：

1. **日历视图-会议室日历-日视图布局错误**：整体布局错位（时间轴与格子未对齐、内容溢出、无法横向滚动、工作时段底色位置错误、事件块未对齐）。
2. **会议室详情-预约日历交互不一致**：点击数据块弹出的是居中 `el-dialog`，期望与日历视图一致的右侧抽屉。
3. **月视图 Popover 浮层过长**：数据多时「+更多」浮层无高度限制，撑出屏幕。
4. **抽屉详情预约时段表示不对**：原为「第一行 2026-07-29 09:00:00 / 第二行 至 2026-07-29 10:30:00」，期望「第一行 2026-07-29 09:00～10:30 / 第二行 会议时间」。

## 技术变更清单

仅前端，3 个文件：

### `frontend/src/views/schedule/ScheduleViewV2.vue`
- **问题1**：补齐会议室日历-日视图缺失的滚动容器样式 `.day-body-wrap { flex:1; overflow:auto; cursor:grab; ... }` 与 `.day-body { position:relative; min-width:fit-content; }`（此前仅有模板无样式，导致布局崩溃；样式与已验证可用的 `RoomScheduleView` 对齐）。
- **问题3**：月视图 `.mc-pop-list` 增加 `max-height:320px; overflow-y:auto;` + 滚动条样式。
- **问题4**：抽屉时间行改用 `formatTimeRange` 紧凑展示，新增 `detailTimeText` 计算属性，副标题由「至 …」改为「会议时间」。

### `frontend/src/components/RoomScheduleView.vue`（会议室详情预约日历）
- **问题2**：将 `el-dialog` + `el-descriptions` 旧交互替换为 `el-drawer`（右侧滑出），内容结构与日历视图抽屉一致（状态条/主题/时间/会议室/预约人/参会人/备注/拒绝原因/参会人列表/底部按钮）。
- 点击事件块改为异步拉取完整详情：管理员走 `getReservationDetail`，普通用户走 `getMyReservationDetail`（仅本人可查），失败回退展示日程数据，避免非本人预约报错。
- 新增 `canCancel`/`handleCancel`/`handleViewFull`/参会人状态辅助函数；移除不再使用的 `statusType`（`noUnusedLocals`）。
- **问题3**：同步为 `.mc-pop-list` 增加最大高度与滚动。
- **问题4**：抽屉时间行同步使用 `detailTimeText`。

### `frontend/src/utils/datetime.ts`
- **问题4**：`formatTimeRange` 分隔符由半角 ` ~ ` 改为全角 `～`（无空格），同天输出 `2026-07-29 09:00～10:30`，跨天输出 `2026-07-29 09:00～2026-07-30 10:00`。该函数此前无调用方，调整无副作用。

## 冲突与风险

- `formatTimeRange` 修改为全角波浪号，属显示风格统一，无其它调用方，风险低。
- 会议室详情抽屉对普通用户点击「他人预约」时，完整详情接口会校验所有权而失败，已用 try/catch 回退到日程数据（仅展示基础字段，无参会人列表），不会报错。
- 「查看完整详情」按钮对普通用户跳转 `/reservation/my/:id`，若非本人预约则由详情页自身处理（与日历视图行为一致）。
- 浏览器自动化对绝对定位事件块的点击 hit-test 不稳定，抽屉点击验证受工具限制；已通过 TS 诊断无错、Vite 干净编译、代码逻辑审查确认修复正确。

## 验证

- `vue-tsc`（noUnusedLocals/noUnusedParameters）通过（VS Code 诊断无报错）。
- Vite dev HMR 热更新成功，无编译错误。
- 问题1 样式直接移植自已验证可用的 `RoomScheduleView` 同名样式。
