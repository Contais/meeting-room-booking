# Bug 修复：附件URL过期 / 站内信时间格式 / AI助手交互优化

## 需求摘要
1. 附件（头像/会议室图片）presigned URL 过期导致图片失效
2. 站内信时间格式异常（LocalDateTime 带 T 的 ISO 格式）
3. AI 助手多项交互问题：中划线渲染、流式思考效果、错误/停止/删除后输入框禁用、id 字段暴露、引导提问

## 技术变更清单

### 一、附件 URL 过期（Issue 1）
| 文件 | 说明 |
|------|------|
| `mrb-platform/application.yml` | `presigned-expire` 3600s → 604800s（7天），大幅降低过期频率 |
| `frontend/.../UserAvatar.vue` | `<img @error>` 降级为图标/首字母头像，URL 失效时优雅回退 |
| `frontend/.../MainLayout.vue` | 同上，顶栏 + 下拉两处头像均加 @error 降级 |

### 二、站内信时间格式（Issue 2）
| 文件 | 说明 |
|------|------|
| `mrb-meeting/.../ReservationServiceImpl.java` | 通知内容时间从 `LocalDateTime.toString()`（带 T）改为 `yyyy-MM-dd HH:mm` 格式化 |
| `frontend/.../NotificationView.vue` | `createTime` 走 `formatDateTime` 工具函数，不再直接显示 ISO 格式 |

### 三、AI 助手交互优化（Issue 3）
| 子项 | 文件 | 说明 |
|------|------|------|
| 3-1 中划线 | `ChatPanel.vue` | DOMPurify 添加 `FORBID_TAGS: ['del','s','strike']`，`~~text~~` 不再渲染为删除线 |
| 3-2 流式思考 | `ChatPanel.vue` | 首个 chunk 到达前显示打字动画，内容开始流式输出后切换为闪烁光标，避免两个气泡并存 |
| 3-3 输入禁用 | `ChatPanel.vue` | 新增 AbortController + 停止按钮；clearChat/stop/error 均重置 loading；检查 response.ok；空回复提示 |
| 3-4 id 暴露 | `chatbot-system-prompt.md` | 强化展示要求：明确禁止暴露预约 ID/会议室 ID/用户 ID，仅展示预约编号与名称 |
| 3-5 引导提问 | `chatbot-system-prompt.md` + `ChatPanel.vue` | AI 可输出 `:::suggest` 块，前端解析为可点击芯片，点击即发送；正文不渲染该块 |

## 冲突与风险
- presigned URL 延长至 7 天，如使用 Nacos 配置需同步修改 `file.storage.cos.presigned-expire`
- 引导提问依赖 AI 按格式输出 `:::suggest` 块，格式不规范时前端自动忽略（不影响正文）
- 停止生成后已接收内容保留，未接收部分截断

## 手动验证点
1. 头像：上传新头像 → 正常显示；模拟 URL 过期（改错 URL）→ 降级为图标头像，无碎图
2. 站内信：创建预约/审批 → 通知内容时间显示 `2026-07-30 14:00 ~ 15:00`（无 T）；列表 createTime 显示 `2026-07-30 14:00:46`
3. AI 中划线：AI 输出含 `~~文字~~` 的内容 → 显示原始文字，无删除线
4. AI 流式：提问 → 先看到打字动画 → 首字到达后切换为闪烁光标随内容移动
5. AI 停止：流式输出中点击停止按钮 → 输入框立即恢复可用，已输出内容保留
6. AI 删除会话：流式输出中点击删除 → 输入框立即恢复可用
7. AI 错误：模拟后端 500 → 显示 `[请求失败（500），请稍后重试]`，输入框恢复
8. AI 引导提问：AI 回复末尾出现可点击的建议芯片 → 点击即自动发送
