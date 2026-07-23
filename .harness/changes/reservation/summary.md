# 会议室预约 - 需求分析（回顾性）

## 需求描述
用户选择会议室和时间段进行预约，系统进行冲突检测和规则校验，支持预约取消。

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 用户可在会议室详情页提交预约（主题、时间、人数、电话、备注） |
| AC-2 | 系统校验时段冲突，已有预约的时段不可重复预约 |
| AC-3 | 系统校验使用规则（时段、时长、提前天数） |
| AC-4 | 用户可查看自己的预约列表（分页、状态筛选） |
| AC-5 | 用户可取消自己的预约 |
| AC-6 | 管理员可查看全部预约列表 |

## 技术变更
| 变更项 | 文件 | 说明 |
|--------|------|------|
| MeetingRoomReservation 实体 | 新增字段 | subject, attendeeCount, contactPhone, remark |
| ReservationService | 完整实现 | createReservation(冲突检测+规则校验), cancelReservation, listMyReservations, listByRoomAndDate |
| ReservationController | 6个接口 | create, cancel, my, room/{id}/date/{date}, admin/list |
| ReservationVO | 新建 | 含会议室名称 |
| ReservationCreateDTO | 新建 | 预约请求参数 |
| init.sql | 表结构更新 | 预约表新增4个字段 |

## 涉及模块
- mrb-meeting（后端预约服务）
- frontend（预约弹窗、我的预约页面、管理端预约列表）

## 已完成时间
2026-07-22
