# [BUG-010] 前端 UI 修复

## 需求描述
修复 6 个前端 UI 问题，涉及部门管理、菜单管理、预约管理、用户管理和首页。

## 验收标准
1. 部门管理：搜索栏展开/收起按钮可正常切换
2. 部门管理：树形表格展开/收起按钮点击后表格状态正确切换
3. 菜单管理：树形表格展开/收起按钮点击后表格状态正确切换
4. 预约管理/我的预约：收起状态下关键字输入框宽度为 640px（与会议室管理一致）
5. 用户管理：标题 h2 字体大小为 18px（与其他管理页面一致）
6. 首页：统计块无论 4 个或 5 个都均匀占满一行

## 技术变更清单

### 1. DeptManage.vue (部门管理)
- 添加 `ArrowDown`/`ArrowUp` 图标导入
- 在 `search-actions` 区域添加展开/收起切换按钮
- 给 `el-table` 添加 `:key="expandAll"` 强制 re-render
- 重置按钮增加 `filterStatus = undefined` 清理

### 2. MenuManage.vue (菜单管理)
- 给 `el-table` 添加 `:key="expandAll"` 强制 re-render

### 3. ReservationManage.vue (预约管理)
- CSS 修复：`.search-keyword-input` → `.search-item .search-keyword-input`，提高选择器特异性

### 4. MyReservations.vue (我的预约)
- CSS 修复：同上

### 5. UserManage.vue (用户管理)
- 添加缺失的 `.page-header` 和 `.page-header h2` 样式定义

### 6. HomeView.vue (首页)
- 替换 `el-row`/`el-col` 为 CSS flex 布局
- `.stat-row` 改为 `display: flex; gap: 16px`
- `.stat-card` 添加 `flex: 1` 自动均分宽度

## 冲突与风险
- 无后端变更，无数据库变更
- 纯前端样式/交互修复，风险低

## 涉及文件
| 文件 | 操作 |
|------|------|
| `frontend/src/views/admin/DeptManage.vue` | 修改 |
| `frontend/src/views/admin/MenuManage.vue` | 修改 |
| `frontend/src/views/admin/ReservationManage.vue` | 修改 |
| `frontend/src/views/reservation/MyReservations.vue` | 修改 |
| `frontend/src/views/admin/UserManage.vue` | 修改 |
| `frontend/src/views/home/HomeView.vue` | 修改 |

## 提交信息
`fix(ui): 修复部门/菜单管理展开收起、预约搜索宽度、用户标题样式、首页统计块布局`
