# 聊天回复 Markdown 横线样式修复

## 需求摘要

用户反馈：聊天窗口 AI 回复内容中出现横向线条（横线），疑似 bug。

## 根因

`ChatPanel.vue` 使用 `marked`（`{ breaks: true, gfm: true }`）渲染 Markdown。GFM 将 `---` 渲染为 `<hr>`。`.markdown-content` 样式块覆盖了 `p` / `ul` / `ol` / `code` / `pre` / `table` / `blockquote` 等元素，但**缺少 `hr` 规则**，导致浏览器以默认全宽边框 / 外边距渲染 `<hr>`，在消息气泡内显示为突兀的横线。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `frontend/src/components/ChatPanel.vue` | 新增 `.markdown-content :deep(hr)` 规则（`border: none` + `border-top: 1px solid #d2d2d7` + `margin: 0.5rem 0`）；暗色模式新增 `html.dark .markdown-content :deep(hr)`（`border-top-color: var(--border)`），复用 `blockquote` 的配色约定 |

## 冲突与风险

- 纯 CSS 改动，无 API / DB / MQ / 业务逻辑变更。
- 红线 #7（`<script setup>`）不受影响，`ChatPanel.vue` 已合规。
- 亮 / 暗双主题均适配（亮色 `#d2d2d7`，暗色 `var(--border)`，与 `blockquote` 边框一致）。
- `npm run build`（vue-tsc + vite build）通过，无类型错误。
