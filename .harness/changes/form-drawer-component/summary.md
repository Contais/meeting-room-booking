# 表单抽屉组件

## 需求摘要
将管理页面的表单弹窗（el-dialog）改为侧边栏式抽屉组件（FormDrawer），提升用户体验，与聊天窗口风格保持一致。

## 技术变更清单

### 前端
| 文件 | 变更类型 | 说明 |
|------|----------|------|
| FormDrawer.vue | 新增 | 侧边栏式表单抽屉组件，支持 v-model:visible、title、loading、submit 事件 |
| UserManage.vue | 修改 | 将 el-dialog 替换为 FormDrawer |
| RoomManage.vue | 修改 | 将 el-dialog 替换为 FormDrawer |
| MenuManage.vue | 修改 | 将 el-dialog 替换为 FormDrawer |
| DeptManage.vue | 修改 | 将 el-dialog 替换为 FormDrawer |

## 组件特性
- 从右侧滑入，宽度 480px
- 半透明遮罩层，点击遮罩可关闭
- 顶部标题栏 + 关闭按钮
- 中间表单区域（可滚动）
- 底部操作按钮（取消 + 确定）
- 支持 loading 状态
- 动画过渡效果

## 冲突与风险
- 无后端变更
- 无数据库变更
- 纯前端 UI 优化，无功能风险
