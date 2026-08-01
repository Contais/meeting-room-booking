# 043 Spring AI 工具结构化返回 — 技术设计

## 1. 背景

项目当前 Spring AI 版本为 `1.1.3`，工具定义采用 `@Tool` 注解。Spring AI 的 `DefaultToolCallResultConverter` 会将非 `String` 返回值序列化为 JSON 回传给模型。当前工具方法返回 `String` 并依赖 `ToolResponseFormatter` 渲染，导致：

- 工具方法同时承担数据组装与展示文案拼接。
- 新增工具时需同步扩展 formatter 的分支。
- 模型收到的是固定文案，无法按用户问题做更灵活的提炼。

## 2. 目标状态

```
用户提问
  │
  ▼
ChatClient
  │ 注入 @Tool 方法
  ▼
@Tool 方法返回结构化 record
  │ DefaultToolCallResultConverter
  ▼
JSON tool result 回传模型
  │ system prompt 组织展示
  ▼
最终用户可见回复
```

## 3. 接口与数据结构

### 3.1 工具返回类型

统一遵循：

- 查询单对象：返回结果 record，如 `RoomListResult`。
- 查询列表：优先返回 `List<RoomSummary>` / `List<ReservationBriefVO>` 等纯列表 record。
- 写操作：返回 `OperationResult(boolean success, String message)`。
- 参数缺失/业务失败：返回 `ToolResult.ErrorResult(String message)` 或 `OperationResult(false, message)`，不再返回预渲染长文本。

示例：

```java
@Tool(description = "查询所有可用的会议室列表。返回 JSON 数组，字段：name 会议室名称、location 位置、capacity 容量、equipment 设备")
public List<RoomSummary> listAvailableRooms() {
    List<MeetingRoom> rooms = meetingRoomRepository.selectList(
            new LambdaQueryWrapper<MeetingRoom>()
                    .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
    );
    return rooms.stream().map(RoomResolver::toSummary).toList();
}
```

```java
@Tool(description = "取消本人的会议室预约。传入预约记录ID，仅可取消本人创建的预约。返回 success 和 message")
public OperationResult cancelMyReservation(
        ToolContext toolContext,
        @ToolParam(description = "预约记录ID") Long reservationId) {
    Long userId = ToolAuthHelper.requireUserId(toolContext);
    MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
    if (reservation == null) {
        return new OperationResult(false, "预约记录不存在");
    }
    if (!reservation.getUserId().equals(userId)) {
        return new OperationResult(false, "无权取消他人的预约");
    }
    reservationService.cancelReservation(userId, reservationId);
    return new OperationResult(true, "预约 " + reservationId + " 已取消");
}
```

### 3.2 时间格式

工具结果 VO 中不再直接暴露 `LocalTime` / `LocalDateTime`，改为序列化前格式化为：

| 原始类型 | 返回字段格式 |
|----------|--------------|
| `LocalTime` | `HH:mm`，如 `"09:30"` |
| `LocalDateTime` | `yyyy-MM-dd HH:mm`，如 `"2026-07-28 14:30"` |
| 日期范围筛选参数 | 保持工具入参 `yyyy-MM-dd` 不变 |

`ReservationBriefVO` 当前同时被普通 REST API 和 AI 工具使用，本次不改该 VO 的 Java 时间类型；若工具需要格式化后的时间字段，在工具结果 record 中增加 String 时间字段，避免影响现有 REST 接口序列化。

### 3.3 结果 record 精简

建议调整：

| 当前类型 | 调整 |
|----------|------|
| `RoomListResult(title, rooms)` | 去掉 `title`，保留 `List<RoomSummary> rooms`；或直接返回 `List<RoomSummary>` |
| `RoomRecommendResult(date, startTime, endTime, rooms)` | 保留数据字段，时间改为格式化 String |
| `RoomReservationResult(roomName, date, reservations)` | 保留，`ReservationBriefVO` 转成工具专用精简结构，去掉内部字段 |
| `FreeSlotResult(roomName, date, slots)` | `TimeSlot` 的 start/end 改为 `HH:mm` String |
| `RoomStatsResult(stats)` | 保留 `RoomStat(name, count)` |
| `ReservationListResult(title, reservations)` | 去掉 `title` |
| `ReservationHistoryResult(total, confirmed, pending, cancelled, reservations, shown)` | 去掉 `shown`，保留统计与列表；列表使用工具专用精简结构 |
| `DepartmentListResult(departments)` | 保留 `DepartmentBrief(id, name)` |
| `AttendeeListResult(attendees)` | 保留 `AttendeeVO`，字段符合现有脱敏规则 |
| `ToolResult.TextResult` | 保留纯错误/提示场景，新增 `ToolResult.ErrorResult(String message)` 统一错误语义 |

### 3.4 system prompt

在 `chatbot-system-prompt.md` 增加一段说明：

```text
## 【工具返回说明】
- 工具返回结果为 JSON 数据，不代表最终回复文案；请基于 JSON 字段组织回答。
- 时间字段统一为 yyyy-MM-dd HH:mm，时间段字段统一为 HH:mm。
- 回复仍必须遵守展示要求：不得暴露内部 ID，列表使用表格或简洁列表。
```

## 4. ToolResponseFormatter 处理

推荐删除 `ToolResponseFormatter.format(ToolResult)` 及所有格式化方法，避免出现“旧工具仍走 formatter、新工具直接返回 JSON”的双轨实现。

如果确认某些转换方法（如实体 → VO）仍有复用价值，可将 `toBriefVO` / `toBriefVOList` 下沉到 `RoomResolver` 或 `ReservationTool` 私有方法，保持工具层边界清晰。

## 5. 测试与验证

### 5.1 编译

```bash
cd backend
mvn -pl mrb-meeting -am compile
```

### 5.2 手工验证场景

| 场景 | 输入 | 预期 |
|------|------|------|
| 会议室列表 | “有哪些会议室可用？” | 正常列出会议室 |
| 模糊匹配歧义 | 输入名称匹配多间会议室 | AI 提示用户明确指定，不暴露 ID |
| 空结果 | 查询无预约日期 | AI 简洁说明无数据 |
| 创建预约 | 指定会议室/日期/时段 | 创建成功并回显会议室、日期时段、主题 |
| 取消越权 | 尝试取消他人预约 | AI 说明无权操作 |
| 历史统计 | 查询上月会议次数 | 返回统计与简要列表 |
| 参会人邀请 | 按部门邀请 | 返回邀请人数 |
| 时间展示 | 空闲时段/预约列表 | 显示 `HH:mm` / `yyyy-MM-dd HH:mm`，无 ISO `T` |

### 5.3 回归检查

- `ChatController` SSE 流式接口不修改。
- `ToolContext` 用户鉴权逻辑不修改。
- `SpringAIConfiguration` 的工具注册方式不修改。

## 6. 提交与评审

- 按任务拆分提交，提交信息遵循 Conventional Commits：`refactor(meeting): ...`
- 变更文件仅限 `mrb-meeting` AI 工具与 prompt 相关文件。
- 完成后执行 coded-review；涉及工具返回协议变更，建议追加 expert-reviewer。
