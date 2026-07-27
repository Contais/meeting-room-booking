# 035 - 预约审批状态闭环

## 背景与问题

当前预约状态枚举仅有 `PENDING(0) / CONFIRMED(1) / CANCELLED(2)` 三种，存在以下逻辑缺口：

1. **状态语义混淆**：`rejectReservation` 复用 `CANCELLED`，导致"管理员拒绝"与"用户主动取消"无法区分，统计与用户提示均无法差异化。
2. **超时未审批无闭环**：PENDING 状态的预约到达 `start_time` 后无任何机制处理，时段被永久占用，会议室实际空置却无法被他人预约；首页 `pendingApproval` 统计虚高。
3. **审批操作无前置校验**：`approveReservation / rejectReservation` 不校验当前状态是否为 PENDING，可对已确认/已取消的预约重复操作。
4. **并发审批竞争**：两个管理员同时点击通过/拒绝，后到覆盖先到。
5. **前端展示缺 REJECTED**：6 处 `statusText/statusType` 仅映射 0/1/2，新增状态后无法展示。
6. **拒绝无原因记录**：用户不知为何被拒。
7. **删除范围过窄**：`deleteReservation` 仅允许删除 CANCELLED，未考虑 REJECTED。

## 改造范围（Phase 1）

### 状态枚举

- 新增 `REJECTED(3, "已拒绝")`
- 状态流转闭环：
  - `PENDING --admin approve--> CONFIRMED`
  - `PENDING --admin reject--> REJECTED`（记录 reason）
  - `PENDING --超时未审批--> REJECTED`（reason="超时未审批，系统自动拒绝"）
  - `PENDING/CONFIRMED --user cancel--> CANCELLED`（仅 start_time 未过可取消）
  - `CANCELLED/REJECTED` 可被删除

### 后端改动

| 文件 | 改动 |
|------|------|
| `ReservationStatusEnum.java` | 新增 `REJECTED(3, "已拒绝")` |
| `ReservationServiceImpl.java` | `rejectReservation(Long, String)` 改用 REJECTED + reason；approve/reject 增加状态前置校验 + CAS 更新；cancel 禁止取消 REJECTED；delete 允许删除 REJECTED；所有 `ne(CANCELLED)` 改为 `notIn(CANCELLED, REJECTED)` |
| `ReservationService.java` | rejectReservation 签名变更（加 reason） |
| `ReservationController.java` | `/admin/reject/{id}` 接受 `RejectDTO`（含 reason） |
| `RejectDTO.java` | 新增（含 reason 字段） |
| `MeetingRoomReservation.java` | 新增 `rejectReason` 字段 |
| `ReservationVO.java` | 新增 `rejectReason` 字段 |
| `HomeServiceImpl.java` | `ne(CANCELLED)` 改为 `notIn(CANCELLED, REJECTED)` |
| `MeetingRoomServiceImpl.java` | 同上 |
| `MeetingRoomTool.java` | 同上 |
| `MeetingRoomApplication.java` | 新增 `@EnableScheduling` |
| `ReservationScheduleTask.java` | 新增定时任务：每分钟扫描 `status=PENDING AND start_time < now`，CAS 更新为 REJECTED + reason |

### 数据库改动

- 新增 `V1.11__add_reservation_reject_reason.sql`：
  - `ALTER TABLE meeting_room_reservation ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因';`
  - 更新 status 字段注释为 `0-待确认, 1-已确认, 2-已取消, 3-已拒绝`

### 前端改动

| 文件 | 改动 |
|------|------|
| `ReservationManage.vue` | statusText/statusType 新增 3 映射；REJECTED 显示删除按钮；拒绝弹窗支持填写原因 |
| `MyReservations.vue` | statusText/statusType 新增 3 映射；REJECTED 显示删除按钮 |
| `ReservationDetail.vue` | statusText/statusType 新增 3 映射；REJECTED 显示删除按钮；展示 rejectReason |
| `MyReservationDetail.vue` | 同上 |
| `ScheduleView.vue` | statusText/statusType 新增 3 映射 |
| `RoomScheduleView.vue` | statusText/statusType 新增 3 映射 |
| `api/reservation.ts` | rejectReservation 接受 reason 参数 |
| `types/reservation.ts` | Reservation 类型新增 rejectReason 字段 |

## 不在本期范围（Phase 2 待评估）

- COMPLETED 状态流转（end_time 过后自动标记已完成）
- 会议室禁用时批量处理未来预约
- 审批模式切换时联动 PENDING 自动通过
- 审批结果站内信/邮件通知（依赖 task8）
- 日历视图 PENDING 视觉区分（虚线/半透明）

## 风险与兼容性

1. **存量数据兼容**：现有 status=2 的记录保持为 CANCELLED，不回填为 REJECTED（历史数据无法判断是用户取消还是管理员拒绝）。前端展示"已取消"即可。
2. **定时任务幂等**：使用 CAS 更新 `WHERE status=PENDING`，重复扫描无副作用。
3. **冲突检测语义**：改造后 PENDING 与 CONFIRMED 都参与冲突检测（占用时段），REJECTED 与 CANCELLED 都排除（释放时段），逻辑一致。
4. **定时任务性能**：每分钟一次轻量查询，单表扫描 status=0 索引可走 `idx_time_range`，无性能压力。
