# 058 AI 工具预约标识协议修复：预约编号流转 + 按日期筛选

> 修复两个 AI 助手缺口：(1) 邀请/取消/查询参会人时模型拿不到预约数字 ID，只能臆造（日志中多次出现
> `reservationId=14/20260812000014` 等猜测值）；(2) 查看"某天我的预约"时工具无日期参数，
> 模型只能拿全量列表目测过滤。本次让工具协议全程使用预约编号（B 开头），并支持按日期服务端过滤；
> 数字预约 ID 不进入工具 JSON、不出现在聊天回复中。

## 需求描述与验收标准

1. `createReservation` 返回结果携带 `reservationCode`（B 开头预约编号），模型可直接用于后续邀请/取消/查询。
2. `inviteDepartmentAttendees` / `cancelMyReservation` / `listReservationAttendees` 的预约标识参数
   改为接受「预约编号（B 开头）或预约记录 ID」，服务端统一解析，模型不再需要猜测数字 ID。
3. `listMyUpcomingReservations` 支持可选 `date`（yyyy-MM-dd）参数，服务端按开始时间当天过滤。
4. 聊天回复不暴露数字预约 ID：工具 JSON 不包含数字预约 ID，系统提示词继续禁止展示内部 ID，
   预约编号（B 开头）可正常展示。

验收标准：
- 后端编译与测试通过；
- 用日志中出现的编号 `B20260812000014` 调用邀请工具能解析到真实预约（snowflake ID），不再报"预约记录不存在"；
- `listMyUpcomingReservations` 传 `date=2026-08-14` 时仅返回该日预约。

## 背景与根因

- 工具结果（`ReservationBrief`、创建结果）从未包含数字预约 ID，而邀请/取消/查询工具只接受 `Long reservationId`；
- 模型只能从编号尾部猜 ID（如 `14`）或把编号数字段当 ID（如 `20260812000014`），全部落空，
  与服务器日志 22:47:43 / 22:47:50 / 22:48:11 的多次失败一致；
- `listMyUpcomingReservations` 无日期参数，模型无法按用户指定日期服务端过滤。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `model/vo/tool/ReservationToolResults.java` | 新增 `CreateReservationResult(success, message, reservationCode)` record |
| `tools/reservation/ReservationTool.java` | `createReservation` 返回 `CreateReservationResult`（含预约编号）；`inviteDepartmentAttendees`/`cancelMyReservation`/`listReservationAttendees` 预约标识参数改为 String（编号或ID）并新增 `resolveReservation` 服务端解析；`listMyUpcomingReservations` 新增可选 `date` 过滤；工具描述同步更新 |
| `resources/prompt/chatbot-system-prompt.md` | 功能清单补充"按日期筛选"；场景2 改为直接用预约编号邀请；工具返回说明补充 reservationCode 用法与内部 ID 禁止展示 |
| `.harness/changes/058-ai-tool-reservation-code-date/summary.md` | 本次变更追踪 |

## 方案说明

- 采用「编号优先」协议：模型只需要 B 开头预约编号，服务端在 `resolveReservation` 中按编号查库、
  按数字 ID 兜底，避免在工具 JSON 中暴露雪花 ID，同时兼容旧模型输出数字 ID 的情况。
- 日期过滤沿用 `parseDateStart` 语义：`start_time >= 当日 00:00 AND start_time < 次日 00:00`，
  格式非法时忽略过滤（与历史查询一致）。
- `listReservationAttendees` 返回类型保持 `ToolResult`，预约不存在时返回统一错误结构。

## 冲突与风险

- 工具入参类型变化会影响模型调用 JSON schema（reservationId: integer → reservationRef: string），
  属于本次修复目标；提示词已同步引导模型传编号。
- 取消/邀请成功后的 message 由数字 ID 改为预约编号，展示更友好且不泄露内部 ID。
- 无 DB / 前端变更；数字预约 ID 从未进入工具 JSON，聊天不暴露内部 ID 的安全约束不变。
