# 037 日历视图 v2（飞书新版风格）

## 需求摘要

当前 v1 日程视图（`/schedule`）被反馈「鸡肋、不好看」。新增 v2 版本，**不替换 v1**，独立路由 `/schedule/v2`，参考飞书日历新版风格重做：

- 双 Tab：「我的日历」+「会议室日历」（后者仅管理员可见）
- 日 / 周 / 月三种视图模式
- 左侧任务栏（迷你月历 + 多日历勾选 + 工作时段过滤）+ 主视图 + 右侧详情抽屉

详细技术方案见 [design.md](./design.md)。

## 技术变更清单

### 前端（纯新增，零后端改动）

| 类型 | 文件 | 说明 |
|------|------|------|
| 新增 | `views/schedule/ScheduleViewV2.vue` | v2 主组件（飞书新版风格，左栏+主视图+右抽屉） |
| 修改 | `router/index.ts` | 新增 `/schedule/v2` 路由（与 v1 共存） |
| 修改 | `views/schedule/ScheduleView.vue` | v1 顶部加「体验新版」入口按钮，跳转 v2 |

### 后端

无改动。复用现有接口：
- 「会议室日历」Tab：`GET /api/meeting/reservation/schedule`（v1 同款）
- 「我的日历」Tab：`GET /api/meeting/reservation/my`（按 startTime/endTime 范围查询，size=500）
- 详情抽屉：`GET /api/meeting/reservation/detail/{id}` / `admin/detail/{id}`

## 接口协议

无新增接口。

## 冲突与风险

- **v1/v2 共存**：v1 保留不删，避免破坏既有用户习惯；v1 顶部加「体验新版」入口引导。
- **我的日历全量查询**：`listMyReservations` 是分页接口，v2 通过 `size=500` + 时间范围查询获取当期数据；超大数据集场景未优化（后续可补 `/my-range` 接口）。
- **管理员 Tab 鉴权**：复用 `userStore.isAdmin()` 判断，仅管理员可见「会议室日历」Tab。
- **路由 / 菜单**：菜单由后端动态加载，v2 入口通过 v1 顶部按钮跳转，不依赖菜单表新增记录。

## 红线自检

- [x] 价格字段 N/A
- [x] Redis Key 前缀 N/A
- [x] MQ 消费者幂等 N/A
- [x] 异常走 BusinessException 体系 N/A（纯前端）
- [x] Controller 构造器注入 N/A
- [x] @Transactional rollbackFor N/A
- [x] Vue 3 `<script setup>`（ScheduleViewV2.vue 使用 `<script setup lang="ts">`）
- [x] API 响应 `{code, message, data}`（复用现有接口，已合规）
