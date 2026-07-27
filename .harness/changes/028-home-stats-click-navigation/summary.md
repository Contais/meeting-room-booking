# 首页统计数据块点击跳转

## 需求描述

首页（`HomeView.vue`）顶部的统计数据卡片当前不可点击。需求：为每个统计块增加点击跳转到合适的页面。

## 现状

`statItems` 计算属性生成统计卡片（会议室 / 今日预约 / 本周预约 / 总预约数 / 待审批[仅管理员]），`.stat-card` 无点击事件、无 `cursor: pointer`。

## 跳转映射建议

| 统计块 | 跳转路径 | 理由 |
|--------|---------|------|
| 会议室 | `/meeting/rooms` | 会议室列表（所有用户可见） |
| 今日预约 | `/reservation/my` | 我的预约（用户视角） |
| 本周预约 | `/reservation/my` | 我的预约 |
| 总预约数 | `/reservation/my` | 我的预约 |
| 待审批（管理员） | `/admin/reservations` | 预约管理（审批入口） |

> 备选：管理员视角下「会议室」可跳 `/admin/rooms`，但为统一体验建议均跳用户视角的 `/meeting/rooms`；待审批跳 `/admin/reservations`。

## 验收标准

| AC | 描述 |
|----|------|
| AC-1 | 每个统计卡片可点击，鼠标悬停显示 `cursor: pointer` 与轻微 hover 反馈 |
| AC-2 | 点击后跳转到上表对应页面 |
| AC-3 | 待审批卡片仅管理员可见且可点击跳转 `/admin/reservations` |
| AC-4 | 普通用户不显示待审批卡片，其余卡片跳转正常 |
| AC-5 | 跳转目标页面存在且权限正确 |

## 技术变更清单

| 类型 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend/src/views/home/HomeView.vue` | `statItems` 每项增加 `path` 字段；`.stat-card` 绑定 `@click="$router.push(item.path)"`；样式增加 hover 与 cursor |

### 改动示意

```ts
const statItems = computed(() => [
  { label: '会议室', value: stats.roomCount, icon: OfficeBuilding, bg: '...', path: '/meeting/rooms' },
  { label: '今日预约', value: stats.todayReservations, icon: Calendar, bg: '...', path: '/reservation/my' },
  { label: '本周预约', value: stats.weekReservations, icon: Clock, bg: '...', path: '/reservation/my' },
  { label: '总预约数', value: stats.totalReservations, icon: DataLine, bg: '...', path: '/reservation/my' },
  ...(userStore.isAdmin() ? [{ label: '待审批', value: stats.pendingApproval, icon: Bell, bg: '...', path: '/admin/reservations' }] : []),
])
```

```css
.stat-card { cursor: pointer; transition: all 0.2s; }
.stat-card:hover { border-color: var(--primary); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
```

## 业务影响范围

- 影响模块：首页
- 用户角色：所有登录用户

## 冲突与风险

- 风险：普通用户点击「会议室」跳 `/meeting/rooms` 需确认该路由对所有用户开放（当前 `meeting/rooms` 路由仅 `requiresAuth: true`，无 admin 限制 ✅）
- 红线检查：无违反

## 任务拆分建议

1. `statItems` 增加 `path` 字段
2. 模板绑定点击 + hover 样式
3. 验证管理员/普通用户两种视角
