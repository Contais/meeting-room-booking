# 详情页功能

## 需求摘要
为管理端增加用户管理、预约管理、会议室管理的详情页，统一风格。列表页操作列增加详情入口，预约编号可点击跳转详情。

## 技术变更清单

### 后端
| 文件 | 变更类型 | 说明 |
|------|----------|------|
| ReservationService.java | 新增方法 | `getReservationDetail(Long)` 查询预约详情 |
| ReservationServiceImpl.java | 新增实现 | 查询预约记录、会议室名称、用户名，组装 VO |
| ReservationController.java | 新增接口 | `GET /admin/detail/{id}` 管理端预约详情 API |

### 前端
| 文件 | 变更类型 | 说明 |
|------|----------|------|
| UserDetail.vue | 新增 | 用户详情页（头像、信息展示、操作按钮） |
| ReservationDetail.vue | 新增 | 预约详情页（描述列表、审批/取消操作） |
| RoomDetail.vue | 新增 | 会议室详情页（信息展示、编辑/禁用/删除操作） |
| router/index.ts | 修改 | 新增 3 条详情页路由 |
| UserManage.vue | 修改 | 操作列增加详情按钮 |
| ReservationManage.vue | 修改 | 操作列增加详情按钮，预约编号可点击跳转 |
| RoomManage.vue | 修改 | 操作列增加详情按钮 |
| types/user.d.ts | 修改 | UserInfo 增加 email、updateTime 可选字段 |
| types/reservation.d.ts | 修改 | Reservation 增加 updateTime 可选字段 |
| api/reservation.ts | 修改 | 新增 `getReservationDetail` API 函数 |

## 统一风格
- 详情页使用 `page-header`（标题 + 返回按钮）+ `detail-card`（白色圆角卡片）+ `action-bar`（操作按钮）统一布局
- 使用 `el-descriptions` 组件展示详情信息
- 头部区域使用大头像/图标 + 名称 + 状态标签

## 冲突与风险
- 无数据库变更，无冲突风险
