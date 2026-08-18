# 菜单管理工具栏按钮功能补全

## 需求描述
菜单管理页右上角有「刷新 / 排序 / 全屏 / 列设置」四个工具栏按钮，其中只有「刷新」绑定了点击事件，「排序 / 全屏 / 列设置」为无事件的占位按钮。需要将这些按钮接入实际功能。

## 根因分析
`frontend/src/views/admin/MenuManage.vue` 在 V2 风格改造时，四个按钮从模板中一并复制为占位按钮，但后续只保留了「刷新」的 `@click`：

- 「排序」按钮：无 `@click`
- 「全屏」按钮：无 `@click`
- 「列设置」按钮：无 `@click`
- 表格数据直接绑定 `filteredData`，没有排序中间状态
- 表格列全部静态渲染，没有列显隐状态

## 验收标准
1. 点击「排序」可在按排序号升序 / 降序之间切换，树节点按层级递归排序
2. 点击「全屏」可让菜单表格卡片进入 / 退出浏览器全屏
3. 点击「列设置」可显示 / 隐藏表格列，且至少保留一列
4. 前端 `npm run build` 通过

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `frontend/src/views/admin/MenuManage.vue` | 为排序 / 全屏 / 列设置按钮补充交互；表格数据改为 `displayData` 递归排序；表格列增加显隐控制；补充 `sortOrder` 排序号列 |

## 关键实现
- 排序：`sortMode`（`asc` / `desc`）+ `cloneAndSortTree`，按 `sortOrder` 排序，相同排序号时按 `createTime` 兜底；`SortUp` / `SortDown` 图标随状态切换
- 全屏：`tableCardRef.requestFullscreen()` / `document.exitFullscreen()`，监听 `fullscreenchange` 同步 `isFullscreen`
- 列设置：`el-popover` + `el-checkbox-group`，`visibleColumns` 控制 `el-table-column` 的 `v-if`，取消所有列时自动恢复「菜单名称」列

## 冲突与风险
- 纯前端改动，无后端 / 数据库 / 接口变更
- 新增「排序号」列使排序效果可见；如产品不希望在列表中展示该列，可在「列设置」中隐藏
- 全屏依赖浏览器 Fullscreen API，若浏览器或 iframe 环境受限会提示失败

## 提交信息
`fix(menu): 菜单管理工具栏排序/全屏/列设置按钮接入功能`
