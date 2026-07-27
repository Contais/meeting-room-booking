# SearchBar 共享组件抽取与搜索 UI 优化

## 需求描述
所有列表页搜索模块的交互与样式风格不统一、视觉粗糙。抽取共享 `SearchBar` 组件统一搜索栏的布局、展开/收起逻辑、按钮样式，并对字段排版做响应式适配。

## 验收标准
1. 所有列表页使用统一的 `<SearchBar>` 组件
2. 折叠/展开状态由组件内部管理，页面移除 `expanded` ref 与 `toggleExpand` 函数
3. 字段容器（`.search-item`）样式提取至全局 `global.css`，移除各页面重复样式
4. 折叠态：单行输入框 + 搜索图标
5. 展开态：3 列网格（响应式：1100px → 2 列，720px → 1 列）
6. 重置/查询按钮带图标 + Tooltip
7. 展开/收起按钮带方向图标旋转动画
8. 删除废弃的 `FilterBar.vue`（已被 SearchBar 替代）

## 技术变更清单

### 新增
| 文件 | 说明 |
|------|------|
| `frontend/src/components/SearchBar.vue` | 共享搜索栏组件：折叠/展开双 slot、统一按钮、响应式网格 |

### 修改
| 文件 | 变更 |
|------|------|
| `frontend/src/styles/global.css` | 新增 `.search-item` 全局样式（label、输入框宽度、圆角） |
| `frontend/src/views/admin/ReservationManage.vue` | 替换为 SearchBar，移除 expanded/toggleExpand 与重复样式 |
| `frontend/src/views/admin/RoomManage.vue` | 同上 |
| `frontend/src/views/admin/UserManage.vue` | 同上 |
| `frontend/src/views/admin/DeptManage.vue` | 同上 |
| `frontend/src/views/admin/MenuManage.vue` | 同上 |
| `frontend/src/views/meeting/RoomListView.vue` | 同上 |

### 删除
| 文件 | 原因 |
|------|------|
| `frontend/src/components/FilterBar.vue` | 从未被任何页面引用，已被 SearchBar 完全替代 |

## SearchBar 组件 API
```vue
<SearchBar @search="..." @reset="..." :default-expanded="false">
  <template #collapsed><!-- 折叠态：单行关键字输入 --></template>
  <template #expanded><!-- 展开态：多字段网格，每个字段包裹在 .search-item 内 --></template>
</SearchBar>
```

- Props: `defaultExpanded?: boolean`
- Events: `search`、`reset`、`expand-change`

## UX 改进点
1. **视觉层级**：折叠态搜索图标提示位置；展开态 label 居顶（小字号 + letter-spacing）
2. **交互反馈**：重置按钮带 RefreshLeft 图标 + Tooltip；查询按钮带 Search 图标；展开图标旋转动画
3. **响应式布局**：3 列 → 2 列 → 1 列自适应
4. **样式收敛**：6 个页面共减少 ~150 行重复 CSS
5. **状态收敛**：expanded 状态由组件持有，页面只关心业务查询逻辑

## 冲突与风险
- 纯前端组件抽取，无后端/DB 变更
- 各页面行为与抽取前完全一致（已通过 vue-tsc + vite build 验证）

## 涉及文件
共 9 个文件改动（新增 1 个组件 + 1 个 summary，修改 7 个，删除 1 个）。

## 提交信息
`refactor(ui): 抽取共享 SearchBar 组件统一列表搜索栏交互与样式`
