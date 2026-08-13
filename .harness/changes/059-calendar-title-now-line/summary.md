# 059 - 日历视图标题统一 + 周视图当前时间线层级修复

## 需求描述

修复日历视图两个前端问题：

1. 侧边栏菜单显示「日历视图」，进入 `/schedule` 后顶栏标题却显示「日程视图」，命名不统一。
2. 日视图和周视图都有当前时间线，但周视图时间线 `z-index` 低于事件块，视觉上被数据块遮挡。

## 验收标准

| AC | 描述 |
|----|------|
| AC-1 | `/schedule` 顶栏标题与菜单一致，显示「日历视图」 |
| AC-2 | 周视图当前时间线覆盖在事件块上方，不再被数据块遮挡 |
| AC-3 | 会议室详情预约日历中的周视图时间线同样显示在事件块上方 |

## 技术变更清单

仅前端，3 个文件：

| 类型 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend/src/router/index.ts` | `/schedule` 路由 `meta.title` 由「日程视图」改为「日历视图」，与菜单名称一致 |
| 修改 | `frontend/src/views/schedule/ScheduleViewV2.vue` | `.wk-now-line` 的 `z-index` 由 `1` 提升到 `3`，高于 `.week-event` 的 `z-index: 2` |
| 修改 | `frontend/src/components/RoomScheduleView.vue` | 同步修复会议室详情预约日历周视图 `.wk-now-line` 的 `z-index` |

## 业务影响范围

- 影响模块：日历视图 `/schedule`、会议室详情预约日历
- 用户角色：所有登录用户
- 无 API、数据库、缓存、MQ 变更

## 冲突与风险

- 「日历视图」已在后端菜单数据中确认（`backend/sql/init.sql`、`V1.4__rename_schedule_menu.sql`），本次统一为前端路由标题。
- `z-index: 3` 不会改变事件块点击与拖拽行为，时间线本身为 `pointer-events: none`。
- 红线检查：仅前端模板/样式与路由元信息变更，无红线违规。

## 验证

- 前端 `npm run build`（`vue-tsc -b && vite build`）通过。
