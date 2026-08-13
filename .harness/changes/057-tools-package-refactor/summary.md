# 057 AI 工具包结构整理

> `com.meetinghub.meeting.tools` 包内同时混有领域工具（会议室/预约/公共信息）、共享解析器与基础设施
> （鉴权助手、日志装饰器），按领域与职责拆分到子包，提升可维护性。

## 需求描述与验收标准

1. 将 `com.meetinghub.meeting.tools` 下类按领域/职责整理为子包：
   - `tools/common`：公共信息工具（当前日期/时间）
   - `tools/meeting`：会议室域工具与共享会议室解析器
   - `tools/reservation`：预约域工具
   - `tools/support`：工具基础设施（鉴权助手、日志装饰器与 Provider）
2. 仅调整包结构与 import，不改变任何工具方法签名、`@Tool` 描述与 AI 可见行为。

验收标准：
- 后端编译与测试通过；
- 工具名/入参/返回结构与重构前完全一致（工具名由方法名推导，不受包路径影响）；
- 组件扫描（`scanBasePackages = com.meetinghub`）覆盖新子包，Bean 正常装配。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `tools/common/CommonTool.java` | 由 `tools` 移入，包名改为 `...tools.common` |
| `tools/meeting/MeetingRoomTool.java` | 由 `tools` 移入，包名改为 `...tools.meeting` |
| `tools/meeting/RoomResolver.java` | 由 `tools` 移入，包名改为 `...tools.meeting` |
| `tools/reservation/ReservationTool.java` | 由 `tools` 移入，包名改为 `...tools.reservation` |
| `tools/support/ToolAuthHelper.java` | 由 `tools` 移入，包名改为 `...tools.support` |
| `tools/support/LoggingToolCallback.java` | 由 `tools` 移入，包名改为 `...tools.support` |
| `tools/support/LoggingToolCallbackProvider.java` | 由 `tools` 移入，包名改为 `...tools.support` |
| `config/SpringAIConfiguration.java` | 更新工具类 import |
| `controller/ChatController.java` | 更新 `ToolAuthHelper` import 与 javadoc 引用 |
| `.harness/changes/057-tools-package-refactor/summary.md` | 本次变更追踪 |

## 方案说明

- 按领域分包：会议室（meeting）、预约（reservation）、公共信息（common）；跨工具共享的鉴权与日志
  基础设施归入 support。
- Spring AI 工具名由 `@Tool` 方法名推导（如 `createReservation`），与类所在包无关，迁移不影响模型调用协议。
- `MeetingRoomTool`/`ReservationTool`/`RoomResolver` 之间的跨包引用已补 import。

## 冲突与风险

- 纯结构调整，无业务行为变化；工具日志（056）继续生效。
- 无 DB / API / 前端变更。
