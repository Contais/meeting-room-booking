# 预约审批流程 - 需求分析（回顾性）

## 需求描述
管理员对需审批模式下的预约进行审批/拒绝操作。

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 管理员可查看全部预约列表 |
| AC-2 | 待确认状态的预约可审批通过（状态变为已确认） |
| AC-3 | 待确认状态的预约可拒绝（状态变为已取消） |
| AC-4 | 已确认状态的预约可被管理员取消 |
| AC-5 | 前端操作按钮根据状态动态显示 |

## 技术变更
| 变更项 | 文件 | 说明 |
|--------|------|------|
| ReservationService | approveReservation/rejectReservation | 状态变更 |
| ReservationController | PUT admin/approve/{id}, admin/reject/{id} | 管理接口 |
| ReservationManage.vue | 操作列 | 通过/拒绝/取消按钮 |

## 涉及模块
- mrb-meeting（后端审批接口）
- frontend（管理端预约管理页面）

## 已完成时间
2026-07-22
