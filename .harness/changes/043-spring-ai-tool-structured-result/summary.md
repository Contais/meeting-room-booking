# Spring AI 工具结构化返回改造 - 需求分析

## 需求描述

当前 `mrb-meeting` 的 Spring AI 工具虽然已使用 `ToolResult` 系列 VO 组织数据，但所有 `@Tool` 方法仍通过 `ToolResponseFormatter` 将 VO 渲染成面向用户的 String 后返回。该方式存在两个问题：

1. 工具层承担了不必要的展示文案拼接逻辑，后续新增工具时容易产生格式不一致和重复代码。
2. Spring AI 的 `DefaultToolCallResultConverter` 本身支持将非 String 返回值序列化为 JSON 回传给模型，当前实现绕过了这一能力，模型收到的是预渲染文本而非结构化数据。

本次改造目标：让 `@Tool` 方法直接返回结构化 record/POJO，由 Spring AI 自动序列化为 JSON；`ToolResponseFormatter` 不再作为工具返回值的必经环节；展示文案统一交给 system prompt 与模型组织。

## 验收标准

| 编号 | 验收条件 |
|------|----------|
| AC-1 | `MeetingRoomTool` 与 `ReservationTool` 的所有 `@Tool` 方法返回结构化对象，不再调用 `ToolResponseFormatter.format(...)` |
| AC-2 | 工具返回的 JSON 字段为纯业务数据，不包含内部 ID、实体字段或展示标题 |
| AC-3 | 系统提示词明确说明各工具返回 JSON 的时间格式与展示规则，AI 回复仍按现有规则用表格/简洁文本输出 |
| AC-4 | 工具错误/参数缺失/权限校验失败返回统一结构化结果，模型能据此给出明确提示 |
| AC-5 | 既有 AI 对话功能不回归：会议室查询、预约创建/取消、参会人邀请等场景仍可正常完成 |
| AC-6 | `ToolResponseFormatter` 删除或降级为非工具链路工具，工具方法不再依赖它 |

## 技术变更清单

| 变更项 | 说明 |
|--------|------|
| 工具方法返回类型 | `MeetingRoomTool` / `ReservationTool` 的 `@Tool` 方法从 `String` 改为直接返回结果 record / `List<record>` / `OperationResult` |
| 结果 VO 精简 | `RoomToolResults`、`ReservationToolResults` 去掉 `title`、`shown` 等展示字段，时间字段改为稳定格式 String，避免依赖 Jackson 默认时间序列化 |
| 统一错误结果 | 新增 `ToolResult.ErrorResult` 或等价结构，替代各方法内 `TextResult` + formatter 的拼装 |
| 工具描述 | 在 `@Tool(description=...)` 中补充返回数据与时间格式说明，帮助模型正确解释 JSON |
| system prompt | 在 `chatbot-system-prompt.md` 中补充“工具返回 JSON 字段与时间格式”说明，保留现有输出规则 |
| `ToolResponseFormatter` | 删除其格式化入口，或保留但仅用于调试/日志，不再被工具方法调用 |

## 影响范围

- `backend/mrb-meeting/.../tools/MeetingRoomTool.java`
- `backend/mrb-meeting/.../tools/ReservationTool.java`
- `backend/mrb-meeting/.../model/vo/tool/RoomToolResults.java`
- `backend/mrb-meeting/.../model/vo/tool/ReservationToolResults.java`
- `backend/mrb-meeting/.../model/vo/tool/ToolResult.java`
- `backend/mrb-meeting/.../tools/ToolResponseFormatter.java`
- `backend/mrb-meeting/src/main/resources/prompt/chatbot-system-prompt.md`

不影响数据库、API 接口、前端页面、MQ、Redis 或权限链路。

## 冲突与风险

| 风险 | 影响 | 应对 |
|------|------|------|
| 模型对 JSON 字段理解不稳定 | AI 回复可能遗漏字段或格式变化 | `@Tool` 描述和 system prompt 明确字段含义、时间格式、展示规则 |
| 时间类型默认序列化为 ISO-8601 | `LocalTime` 变成 `"14:30:00"`、`LocalDateTime` 带 `T` | 工具结果 VO 中时间字段改为 `HH:mm` / `yyyy-MM-dd HH:mm` String |
| 移除 formatter 后回归 | 某个工具返回结构不完整导致 AI 答错 | 按验收标准做全场景手动验证，重点覆盖模糊会议室、空列表、操作失败 |
| Spring AI 1.1.x 序列化差异 | 不同模型/版本对 JSON 解析不一致 | 本次不升级 Spring AI，保持 1.1.3；如后续升级到 2.x 再按 2.x 文档调整 |

## 任务拆分建议

1. 精简工具结果 VO，新增统一错误结果
2. 改造 `MeetingRoomTool` 为直接返回结构化对象
3. 改造 `ReservationTool` 为直接返回结构化对象
4. 更新 system prompt 与 `@Tool` 描述
5. 移除/降级 `ToolResponseFormatter`，清理无用 import
6. 编译 + 全场景对话验证
