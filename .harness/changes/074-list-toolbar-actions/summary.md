# 全部标准列表页工具栏动作统一接入

## 需求描述
菜单管理页已经补齐「刷新 / 排序 / 全屏 / 列设置」工具栏按钮。为避免各列表页各自维护一套相同逻辑，需要将同样的能力推广到其余标准列表页。

## 影响页面
- 用户管理
- 设备管理
- 会议室管理
- 知识库管理
- 预约管理
- 角色管理
- 部门管理
- 我的预约
- 我的会议

## 技术方案
1. 新增 `TableToolbarActions.vue`，统一渲染刷新、排序、全屏、列设置四个按钮，并处理全屏状态与列设置兜底。
2. 新增 `useTableToolbar` 组合式函数，集中维护 `sortOrder`、`visibleColumns`、`isColumnVisible`。
3. 新增 `utils/table.ts`，提供扁平列表与树形列表的本地排序工具。
4. 各列表页使用统一组件替换原「刷新」按钮，并给数据列增加显隐控制。

## 验收标准
1. 所有目标列表页均显示刷新、排序、全屏、列设置按钮
2. 刷新、排序、全屏、列设置在目标页面均可用
3. 列设置取消所有列时会保留第一列
4. `frontend` 生产构建通过

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `frontend/src/components/TableToolbarActions.vue` | 新增统一工具栏动作组件 |
| `frontend/src/composables/useTableToolbar.ts` | 新增排序/列显隐组合式函数 |
| `frontend/src/utils/table.ts` | 新增扁平列表和树形列表排序工具 |
| `frontend/src/types/table.ts` | 新增排序与列配置类型 |
| `frontend/src/styles/global.css` | 新增全屏目标容器样式 |
| `frontend/src/views/admin/UserManage.vue` | 接入统一工具栏与列显隐 |
| `frontend/src/views/admin/EquipmentManage.vue` | 同上 |
| `frontend/src/views/admin/RoomManage.vue` | 同上 |
| `frontend/src/views/admin/KnowledgeManage.vue` | 同上 |
| `frontend/src/views/admin/ReservationManage.vue` | 同上 |
| `frontend/src/views/admin/RoleManage.vue` | 同上 |
| `frontend/src/views/admin/DeptManage.vue` | 树形列表接入统一工具栏与递归排序 |
| `frontend/src/views/reservation/MyReservations.vue` | 接入统一工具栏与列显隐 |
| `frontend/src/views/meeting/MyMeetingsView.vue` | 同上 |

## 排序字段说明
- 用户 / 设备 / 会议室 / 预约管理 / 我的预约：按创建时间排序
- 知识库 / 角色：按排序字段排序
- 部门：按部门排序号递归排序
- 我的会议：按会议开始时间排序

## 冲突与风险
- 排序为当前页客户端排序，分页列表仅影响当前页数据，不改变后端排序结果
- 全屏依赖浏览器 Fullscreen API
- 纯前端改动，无后端 / 数据库 / 接口变更

## 提交信息
`feat(ui): 全列表页接入排序/全屏/列设置工具栏动作`
