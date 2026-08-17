# 069 统一品牌主色（#409eff → 品牌靛蓝）

## 需求描述

系统在视觉上存在「两套蓝」：全局品牌主色为靛蓝 `#4f6ef7`（`--primary`），
但部分自定义组件仍硬编码 Element Plus 默认蓝 `#409eff` 及其浅/深变体。
在页面间切换（如新版主日历 vs 会议室详情/预约抽屉）时会出现明显色相跳动，
削弱专业观感。

## 验收标准

- 前端源码中不再出现硬编码的 `#409eff` 及其 rgba 变体（`rgba(64,158,255,…)`）。
- 相关交互态（hover / active / today / 选区 / 主按钮）统一使用品牌靛蓝 `--primary` 或对应明度 token。
- 暗色模式下的观感不劣化。

## 技术变更清单

仅前端样式层，无 DB / API / 缓存 / MQ 变更。

| 文件 | 变更 |
|------|------|
| `frontend/src/components/RoomScheduleView.vue` | 控制按钮、今日高亮、月视图更多、抽屉按钮改用 `--primary` |
| `frontend/src/components/BookingDialog.vue` | 时间轴选区、时长标签、箭头改用品牌靛蓝及浅色变体 |
| `frontend/src/components/RoomCalendar.vue` | 今日日期高亮改用 `--primary` |
| `frontend/src/views/admin/RoomDetail.vue` | 渐变改用品牌靛蓝 `--primary` → `--el-color-primary-dark-2` |
| `frontend/src/components/UserSelectDialog.vue` | 回退色由 `#409eff` 修正为品牌靛蓝 `#4f6ef7` |
| `frontend/src/layouts/MainLayout.vue` | 回退色由 `#409eff` 修正为品牌靛蓝 `#4f6ef7` |

## 冲突与风险

- 低风险：纯 CSS 色值替换，不影响逻辑与布局。
- `--primary` 在亮/暗模式均保持 `#4f6ef7`，替换后行为一致。
