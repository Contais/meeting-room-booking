# 侧边栏 / 顶栏 / 聊天入口 / 个人中心 UI 修复

## 需求摘要

修复四项前端 UI 问题：
1. 侧边栏折叠时 logo 未居中 + 亮色模式下二级菜单弹出层文字不可见
2. 顶部栏按钮间距过宽
3. 聊天入口缺少在线状态指示
4. 个人中心页面过于简陋

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `frontend/src/layouts/MainLayout.vue` | logo 补 `width: 100%`；`.header-right` gap 12px→8px；AI 助手按钮加 `chat-btn` 类 + `::after` 绿色在线圆点 + 呼吸动画 |
| `frontend/src/styles/global.css` | 新增亮色模式 `.el-menu--popup` 深色背景 + 白色文字规则（修复折叠态二级菜单白字不可见） |
| `frontend/src/views/user/ProfileView.vue` | 新增横幅卡片（头像+姓名+角色）；表单卡片头部加图标；补 `padding: 24px`；硬编码色改为 CSS 变量 |

## 冲突与风险

- 无 API / DB / MQ 变更，纯 CSS + 模板改动。
- 亮色模式弹出层背景 `#302b63` 与侧栏渐变一致；`html.dark` 特异性更高，暗色模式不受影响。
- 所有颜色使用 CSS 变量，亮/暗双主题均适配。
- `npm run build`（vue-tsc + vite build）通过，无类型错误。
