# 全仓统一 DateTimePatternConstant 限定名引用

> 将全仓对 `DateTimePatternConstant` 成员的引用统一为限定名 `DateTimePatternConstant.X` 直接使用：
> 删除所有 `import static`，并移除各文件中的「本地别名字段」，消除冗余间接层，代码风格全仓一致。

## 需求摘要

1. **统一引用风格**：全仓对 `DateTimePatternConstant.DATE_FMT / TIME_FMT / DATETIME_FMT` 的引用一律使用限定名，禁止 `import static`。
2. **消除冗余别名**：`CommonTool`、`ReservationTool`、`MeetingRoomTool`、`ReservationToolResults` 中的 `private static final DateTimeFormatter X = DateTimePatternConstant.X;` 本地别名字段无任何价值，直接使用限定名。
3. **不改变行为**：常量值、字段类型、业务逻辑均不变，纯代码风格重构。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `MeetingRoomTool.java` | 删除 2 行 `import static`，补回普通导入，9 处使用点改限定名（`DATE_FMT` ×3、`TIME_FMT` ×6） |
| `ReservationScheduleTask.java` | 删除静态导入，新增普通导入，2 处 `DATETIME_FMT` 使用点改限定名 |
| `CommonTool.java` | 删除 2 个本地别名字段及不再使用的 `DateTimeFormatter` 导入，2 处使用点改限定名 |
| `ReservationTool.java` | 删除 2 个本地别名字段及不再使用的 `DateTimeFormatter` 导入，5 处使用点改限定名 |
| `ReservationToolResults.java` | 删除 1 个本地别名字段及不再使用的 `DateTimeFormatter` 导入，1 处使用点改限定名 |

## 冲突与风险

- `MeetingRoomTool.java` 工作区存在未提交的 wildcard 导入 `RoomToolResults.*` 改动（既有工作），本次保留并随同提交。
- 纯机械重构，无业务逻辑变更；风险点为遗漏未改的静态导入/裸引用，已通过全局 `rg` 断言覆盖。
- 编译依赖：改后各文件需保留 `DateTimePatternConstant` 普通导入，缺失将导致编译失败，已逐一确认。

## 验收标准

- `rg "import static com.meetinghub.common.constant.DateTimePatternConstant" backend` 无结果。
- 除常量类自身外，无裸 `DATE_FMT` / `TIME_FMT` / `DATETIME_FMT` / `DATE_TIME_FMT` 使用。
- `mvn -pl mrb-meeting/mrb-meeting-service -am test`（或 compile + 单元测试）通过。
