# 056 AI 助手工具调用链路日志完善

> 服务器日志显示 `MethodToolCallback` 的 WARN「Conversion from JSON failed」不含工具名与参数，
> 且 Chat 入口无请求/结束日志，AI 助手"预约已创建但未邀请参会人"这类断链问题无法通过日志初步定位。
> 本次为 AI 工具调用链路补齐关键日志，使后续问题可通过日志快速定位到具体工具与参数。

## 需求描述与验收标准

1. 每次 AI 工具调用输出日志：工具名、原始入参（截断）、耗时、结果摘要；调用抛异常时输出异常堆栈。
2. 工具参数类型转换失败（如把预约编号 `B...` 传入 `Long` 参数）也能通过日志定位到具体工具与原始入参。
3. Chat 请求在入口输出对话开始日志（conversationId、userId、消息摘要），流结束/异常输出对应日志。
4. 不改变工具行为与 ToolContext 传递（`ToolAuthHelper` 依赖工具上下文中的用户身份）。

验收标准：
- 后端编译通过；
- 日志中能同时看到「AI 工具调用开始/完成/失败」与对应的工具名、入参；
- 装饰器正确透传 `ToolContext`，邀请类工具仍能取到当前用户。

## 背景与根因

- `ChatController` 返回 SSE 流（`Flux<String>`），工具调用阶段模型不输出正文，服务端在最终回答前不发字节；
  多轮工具往返叠加 DeepSeek 延迟，容易超过网关/前置代理响应超时返回 504。
- 更关键的是工具协议断链：创建/查询预约的工具结果只有预约编号（B 开头），没有数字预约 ID，
  而 `inviteDepartmentAttendees` 等工具只接受 `Long reservationId`；模型把编号 `B20260812000012`
  直接传入时，Spring AI 反序列化抛 `NumberFormatException`（日志 20:59:15 已证实），
  工具未执行、模型反复重试，最终表现为"预约已创建但参会人为空 + 504"。
- 该 WARN 由 Spring AI 库打印，不包含工具名与参数，无法定位是哪个工具收到了错误入参。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `tools/LoggingToolCallback.java`（新增） | 装饰 `ToolCallback`：记录工具名、入参（截断）、耗时、结果摘要，异常时输出堆栈；透传 `ToolContext` |
| `tools/LoggingToolCallbackProvider.java`（新增） | 基于 `MethodToolCallbackProvider` 生成回调后统一包装为 `LoggingToolCallback` |
| `config/SpringAIConfiguration.java` | `defaultTools(...)` 改为 `defaultToolCallbacks(new LoggingToolCallbackProvider(...))` |
| `controller/ChatController.java` | 增加对话开始/正常结束/异常日志（conversationId、userId、消息摘要） |
| `.harness/changes/056-ai-tool-chain-logging/summary.md` | 本次变更追踪 |

## 方案说明

- Spring AI 1.1.3 的 `defaultTools(Object...)` 只接受带 `@Tool` 方法的对象，
  `ToolCallback` 装饰器必须通过 `defaultToolCallbacks(ToolCallbackProvider...)` 注册，
  因此新增 `LoggingToolCallbackProvider` 包装 `MethodToolCallbackProvider` 的产物。
- `DefaultToolCallingManager` 调用的是 `call(toolInput, toolContext)`，
  装饰器必须透传 `ToolContext`，否则 `ToolAuthHelper.requireUserId` 取不到当前用户。
- 参数/结果统一截断（入参 200、结果 500 字符），避免长列表刷屏。

## 冲突与风险

- 工具注册方式由 `defaultTools` 切换为 `defaultToolCallbacks`，工具集合不变，行为不变。
- 日志新增 INFO/ERROR 量级可控（每次工具调用 1-2 条），入参与结果截断防刷屏。
- 本次仅完善日志，不改变"工具协议缺预约 ID"这一功能缺陷；该缺陷的修复另立变更项。
