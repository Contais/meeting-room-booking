# 新需求：重置密码 + 日历视图

## 需求描述

### 需求 1：用户管理（管理端）增加重置用户密码功能
管理员可以重置指定用户的密码，用于用户忘记密码或账号异常时的密码重置。

### 需求 2：增加日历视图
参考飞书会议室日历视图功能，提供可视化展示会议室预约情况的日历视图：
- 按日期范围展示所有会议室的预约情况
- 支持切换不同视图模式（日/周/月）
- 直观展示预约时段、预约人、会议主题
- 支持点击空白时段快速创建预约

## 验收标准

### 需求 1：重置密码
1. 管理员可在用户管理列表点击"重置密码"按钮
2. 弹出确认对话框，输入新密码
3. 确认后密码立即生效，用户可使用新密码登录
4. 密码需符合安全规范（至少 6 位）

### 需求 2：日历视图
1. 提供日历视图页面，展示所有会议室预约情况
2. 支持日/周/月三种视图模式切换
3. 日历中展示预约色块（颜色区分状态）
4. 点击预约可查看详情
5. 点击空白时段可快速创建预约

## 技术变更清单

### 后端（mrb-user 模块）
| 变更类型 | 文件 | 说明 |
|----------|------|------|
| 修改 | `UserController.java` | 新增重置密码接口 |
| 修改 | `UserService.java` | 新增 resetPassword 方法 |
| 修改 | `UserServiceImpl.java` | 实现重置密码逻辑 |

### 后端（mrb-meeting 模块）
| 变更类型 | 文件 | 说明 |
|----------|------|------|
| 修改 | `ReservationController.java` | 新增日历视图数据接口 |
| 修改 | `ReservationService.java` | 新增 getCalendarData 方法 |
| 修改 | `ReservationServiceImpl.java` | 实现日历数据查询 |

### 前端
| 变更类型 | 文件 | 说明 |
|----------|------|------|
| 新增 | `views/meeting/CalendarView.vue` | 日历视图页面 |
| 修改 | `api/user.ts` | 新增重置密码 API |
| 修改 | `views/admin/UserManage.vue` | 添加重置密码按钮 |
| 修改 | `router/index.ts` | 添加日历视图路由 |

## 冲突与风险
- 无数据库表结构变更
- 重置密码功能涉及用户认证模块
- 日历视图需要高效的时段查询接口

## 涉及文件
| 文件 | 操作 |
|------|------|
| `backend/mrb-user/src/main/java/com/meetinghub/user/controller/UserController.java` | 修改 |
| `backend/mrb-user/src/main/java/com/meetinghub/user/service/UserService.java` | 修改 |
| `backend/mrb-user/src/main/java/com/meetinghub/user/service/impl/UserServiceImpl.java` | 修改 |
| `backend/mrb-meeting/src/main/java/com/meetinghub/meeting/controller/ReservationController.java` | 修改 |
| `backend/mrb-meeting/src/main/java/com/meetinghub/meeting/service/ReservationService.java` | 修改 |
| `backend/mrb-meeting/src/main/java/com/meetinghub/meeting/service/impl/ReservationServiceImpl.java` | 修改 |
| `frontend/src/views/meeting/CalendarView.vue` | 新建 |
| `frontend/src/api/user.ts` | 修改 |
| `frontend/src/views/admin/UserManage.vue` | 修改 |
| `frontend/src/router/index.ts` | 修改 |
