# 055 修复创建预约时参会人数重复计算

> `createReservation` 在创建预约时先按 `attendeeUserIds.size() + 1` 预置 `attendeeCount`，
> 随后 `inviteAttendees` 又按实际插入的参会人行数累加一次，导致有被邀请人时参会人数被重复计算。

## 需求描述与验收标准

1. 创建预约时不再重复计算参会人数：
   - 无被邀请人：`attendeeCount = 1`（仅创建者）；
   - 有被邀请人：`attendeeCount = 1 + 实际插入的参会人数`（去重、排除创建者后）。
2. `inviteAttendees` 在「创建后单独邀请」「按部门邀请」「Controller 邀请」等既有路径上的行为保持不变。
3. `removeAttendee` 移除参会人后，`attendeeCount` 按实际删除行数同步递减，冗余计数与实际参会人行数保持一致。

验收标准：
- 后端编译通过；
- 变更文件新增代码无红线违规（BigDecimal / Redis 前缀 / BusinessException / 构造器注入 / rollbackFor 等均不涉及或保持现状）。

## 背景与根因

- `ReservationServiceImpl.createReservation` 第 96-98 行先按 DTO 原始列表 `size() + 1` 预置 `attendeeCount`；
- 随后第 120 行调用 `inviteAttendees`，`ReservationAttendeeServiceImpl` 第 113 行又执行
  `attendeeCount + toAdd.size()`（`toAdd` 为去重、排除创建者后的实际插入行数）。
- 两条路径叠加导致 `attendeeCount = 1 + 2N`（有 N 个被邀请人时）；无被邀请人时 `inviteAttendees` 不执行，
  故未触发重复计算。
- 附加问题：DTO 列表中若包含创建者自己或重复 id，预计算口径与 `toAdd` 口径不一致，结果同样不准。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `ReservationServiceImpl.java` | 删除 `invitedCount` / `attendeeCount = invitedCount + 1` 预计算，创建时仅初始化 `attendeeCount = 1`（创建者自身），被邀请人数由 `inviteAttendees` 按实际插入行数累加 |
| `ReservationAttendeeServiceImpl.java` | `removeAttendee` 删除参会人成功后按实际删除行数递减 `attendeeCount`（空值按 0、下限 0 防御）；`checkReservationOwnership` 改为返回预约实体供复用，避免重复查询 |
| `.harness/changes/055-attendee-count-fix/summary.md` | 本次变更追踪 |

## 方案说明

- `inviteAttendees` 是共享方法（创建、创建后邀请、部门邀请、Controller 均调用），其「按实际插入行数累加」
  语义必须保留，因此不在该方法内做减法；
- 计数以「实际落库的参会人行数」为准：创建时固定 1（创建者），后续每次插入按 `toAdd.size()` 累加，
  天然规避 DTO 含创建者自身或重复 id 导致的计数偏差。

## 冲突与风险

- `attendeeCount` 是冗余计数，本次已修复「创建时重复计算」，并补齐 `removeAttendee` 的同步递减，
  计数维护收口到 `ReservationAttendeeService`（谁插入行谁 + 实际行数、谁删除行谁 - 实际行数、
  创建时初始化创建者 1 人）。
- 若后续仍担心冗余字段漂移，可将读取侧改为按参会人表动态计数、彻底移除该字段，作为可选演进方向。
- 未涉及 DB / API / 缓存 / MQ / 前端变更，无冲突。
