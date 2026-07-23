# 会议室使用规则 - 需求分析（回顾性）

## 需求描述
为会议室配置使用规则，预约时进行规则校验。

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 管理员可配置可预约时段（如08:00~20:00） |
| AC-2 | 管理员可配置最大预约时长 |
| AC-3 | 管理员可配置提前预约天数限制 |
| AC-4 | 管理员可设置审批模式（免审批/需审批） |
| AC-5 | 预约时后端校验规则，不符合规则时拒绝预约 |
| AC-6 | 前端详情页展示规则信息 |
| AC-7 | 预约弹窗显示规则提示 |

## 技术变更
| 变更项 | 文件 | 说明 |
|--------|------|------|
| MeetingRoom 实体 | 新增5个字段 | bookableStart, bookableEnd, maxDuration, advanceDays, needApproval |
| RoomCreateDTO/RoomUpdateDTO | 新增规则字段 | 对应实体新增字段 |
| MeetingRoomVO | 新增规则字段 | 返回给前端 |
| ReservationServiceImpl | validateRoomRules | 校验时段/时长/提前天数 |
| RoomManage.vue | 弹窗新增规则区域 | 时间选择器 + 输入框 + 单选 |
| RoomDetailView | 规则信息卡片 + 弹窗提示 | 展示规则 |
| init.sql | 表结构更新 | 会议室表新增5个规则字段 |

## 涉及模块
- mrb-meeting（实体、服务、控制器）
- frontend（管理页面、详情页面）

## 已完成时间
2026-07-22
