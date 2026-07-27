# 详情页标题去重 + 顶部面包屑式标题（含父级跳转）

## 需求描述

当前详情页（以预约详情为例）出现「两个标题」：

1. **顶部标题**：`MainLayout.vue` 顶栏的 `page-title`，取自 `route.meta.title`（如「预约详情」）
2. **下方标题**：详情页自身 `page-header` 里的 `<h2 class="page-title">预约详情</h2>`（近期新增）

需求：
- 移除详情页 `page-header` 中的标题，仅保留「返回」按钮与操作按钮
- 顶部标题改为「父级 / 当前」面包屑形式，例如「我的预约 / 预约详情」
- 点击父级部分可跳转到对应父级页面

## 验收标准

| AC | 描述 |
|----|------|
| AC-1 | 所有详情页 `page-header` 不再出现 `<h2>` 标题，仅保留返回按钮 + 操作按钮 |
| AC-2 | 顶栏标题显示「父级标题 / 当前标题」格式 |
| AC-3 | 点击父级标题跳转到对应父级列表页 |
| AC-4 | 当前标题不可点击（仅父级可点击） |
| AC-5 | 无父级的页面（如首页、列表页）顶栏仅显示当前标题，不出现分隔符 |
| AC-6 | 涉及页面：MyReservationDetail、ReservationDetail、UserDetail、RoomDetail、RoomDetailView |

## 技术变更清单

| 类型 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend/src/layouts/MainLayout.vue` | `currentTitle` 计算属性改为读取 `route.meta.parent`，渲染可点击的父级 + 分隔符 + 当前标题 |
| 修改 | `frontend/src/router/index.ts` | 为详情页路由 `meta` 增加 `parent: { path, title }` 字段 |
| 修改 | `frontend/src/views/reservation/MyReservationDetail.vue` | 移除 `page-header` 内 `<h2 class="page-title">` |
| 修改 | `frontend/src/views/admin/ReservationDetail.vue` | 同上 |
| 修改 | `frontend/src/views/admin/UserDetail.vue` | 同上 |
| 修改 | `frontend/src/views/admin/RoomDetail.vue` | 同上 |
| 修改 | `frontend/src/views/meeting/RoomDetailView.vue` | 同上（如存在 page-header 标题） |

### 路由 meta.parent 映射

| 路由 | 当前标题 | 父级 path | 父级标题 |
|------|---------|-----------|---------|
| /reservation/my/:id | 预约详情 | /reservation/my | 我的预约 |
| /admin/reservations/:id | 预约详情 | /admin/reservations | 预约管理 |
| /admin/users/:id | 用户详情 | /admin/users | 用户管理 |
| /admin/rooms/:id | 会议室详情 | /admin/rooms | 会议室管理 |
| /meeting/rooms/:id | 会议室详情 | /meeting/rooms | 会议室列表 |

### MainLayout 渲染方案（伪代码）

```html
<h2 class="page-title">
  <span v-if="parent" class="parent-link" @click="router.push(parent.path)">{{ parent.title }}</span>
  <span v-if="parent" class="separator">/</span>
  <span class="current">{{ currentTitle }}</span>
</h2>
```

## 业务影响范围

- 影响模块：所有详情页 + 主布局顶栏
- 用户角色：所有登录用户

## 冲突与风险

- 风险1：`router.back()` 与父级跳转语义不同——保留返回按钮使用 `router.back()`，父级链接使用 `router.push(parent.path)`，二者并存不冲突
- 风险2：需确认所有详情页 `page-header` 标题移除后样式不塌陷（返回按钮 + 操作按钮仍需两端对齐）
- 红线检查：无违反

## 任务拆分建议

1. 路由 meta 增加 parent 字段（5 个详情路由）
2. MainLayout 顶栏改造为面包屑式标题 + 父级点击跳转
3. 逐个移除详情页 page-header 标题（5 个页面）
4. 验证样式与跳转
