# 角色管理列表页标准化 + 抽取 TableCard 公共组件

## 需求描述

角色管理页面（RoleManage.vue）的布局、样式与其他列表页不一致，需统一为标准列表页模板。同时抽取公共列表组件，便于后续复用。

## 现状问题

| 对比项 | 其他列表页（UserManage/RoomManage 等） | RoleManage |
|--------|---------------------------------------|------------|
| 容器 | `page-view` | `role-manage-page`（自定义） |
| 搜索 | `SearchBar` 组件（折叠/展开） | 内联 `el-input` + 标题 |
| 表格容器 | `table-card` + `table-toolbar` | `role-table-wrapper`（自定义） |
| 分页 | `pagination-wrap` + total-text | 自定义 `pagination-wrapper` |
| 新建/编辑 | `FormDrawer` 抽屉 | `el-dialog` 对话框 |

## 方案

### 1. 新增 `TableCard` 公共组件

路径：`frontend/src/components/TableCard.vue`

封装表格卡片的标准结构：
- `toolbar-left` / `toolbar-right` 插槽：工具栏左右区域
- 默认插槽：表格内容
- 分页：内置 `el-pagination` + total-text，通过 `v-model:page` / `v-model:size` 双向绑定

Props:
- `total`：总条数
- `page` / `size`：当前页/每页条数
- `pageSizes`：可选每页条数（默认 [10, 20, 50]）
- `showPagination`：是否显示分页（默认 true）

Events:
- `update:page` / `update:size`
- `size-change` / `current-change`

### 2. RoleManage 标准化改造

- 容器：`role-manage-page` → `page-view`
- 搜索：内联输入框 → `SearchBar`（支持角色名称/状态/类型筛选）
- 表格：`role-table-wrapper` → `TableCard`
- 新建/编辑：`el-dialog` → `FormDrawer`
- 权限配置：保留 `el-dialog`（角色管理独有，无需标准化）
- 操作列：保持原有按钮样式（按用户要求不改）
- 时间格式化：自定义 `formatDate` → 统一 `formatDateTime` 工具函数

### 3. 其他列表页同步迁移

将 UserManage / RoomManage / ReservationManage / MyReservations 的内联 `table-card` 结构替换为 `TableCard` 组件，消除重复代码。

MenuManage / DeptManage 不迁移（无分页，按项目约定排除）。

## 技术变更清单

| 类型 | 文件 | 说明 |
|------|------|------|
| 新增 | `components/TableCard.vue` | 列表表格卡片公共组件 |
| 重构 | `views/admin/RoleManage.vue` | 标准化为 page-view + SearchBar + TableCard + FormDrawer |
| 修改 | `views/admin/UserManage.vue` | 内联 table-card → TableCard 组件 |
| 修改 | `views/admin/RoomManage.vue` | 内联 table-card → TableCard 组件 |
| 修改 | `views/admin/ReservationManage.vue` | 内联 table-card → TableCard 组件 |
| 修改 | `views/reservation/MyReservations.vue` | 内联 table-card → TableCard 组件 |

## 验收标准

- [x] RoleManage 布局与其他列表页一致
- [x] TableCard 组件可复用，API 清晰
- [x] 所有列表页功能不变（搜索/分页/操作）
- [x] TypeScript 编译通过
- [x] 角色管理操作列样式保持不变

## 冲突与风险

- **风险1**：RoleManage 后端仅支持 keyword 搜索，状态/类型筛选为前端二次过滤（数据量大时可能不准确）
- **风险2**：TableCard 的 v-model:page/size 与各页面原有变量名（page/size vs pageNum/pageSize）需正确绑定
- **风险3**：权限配置对话框保留 el-dialog，与 FormDrawer 混用需验证交互不冲突
