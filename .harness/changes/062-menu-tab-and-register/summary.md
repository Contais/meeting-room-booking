# 062-menu-tab-and-register 需求分析摘要

## 需求描述

三项调整（已与用户确认方案）：

1. **菜单排序/分类（方案一：常用优先排序）**：用户菜单按 首页 → 会议室 → 我的预约 → 我的会议 → 日历视图 → 通讯录；
   管理子菜单按 用户 → 部门 → 角色 → 会议室 → 预约 → 菜单 → 设备；消息通知保留顶部铃铛入口、不进侧边栏。
   同时补齐 `init.sql` 缺失的“设备管理(17)/我的会议(18)”菜单（数据漂移 bug），并统一重复图标。
2. **注册功能不开放**：内部系统由管理员在“用户管理”建号；移除网关白名单中的注册路径，下线
   `POST /api/uc/user/register` 后端接口（避免任意登录用户调用创建账号），前端无注册入口。
3. **浏览器标签页**：标题格式“页面名 - 会议室预约系统”，路由切换自动更新；新增 favicon（会议室图标 + 品牌渐变）。

## 验收标准

- [ ] 全新环境执行 `init.sql` 后，`platform_menu` 共 18 条（含 17 设备管理/18 我的会议），
      `platform_role_menu` 共 20 条（admin 14 条 + user 6 条）
- [ ] 普通用户侧边栏顺序：首页/会议室/我的预约/我的会议/日历视图/通讯录，图标分别为
      HomeFilled/OfficeBuilding/Calendar/Tickets/Grid/UserFilled
- [ ] 管理员“系统管理”子菜单顺序：用户/部门/角色/会议室/预约/菜单/设备
- [ ] 网关白名单不再包含 `/api/auth/register` 与 `/api/uc/user/register`；未登录调用注册接口返回 401
- [ ] 登录页不展示注册入口（现状已满足，不新增）
- [ ] 浏览器标签页显示“页面名 - 会议室预约系统”，favicon 正常加载
- [ ] 菜单管理页图标下拉可选 FolderOpened（部门管理图标）
- [ ] mrb-user 模块单元测试/集成测试通过；前端 `npm run build` 通过

## 技术变更清单

| 变更 | 说明 |
|------|------|
| `backend/sql/init.sql` | 菜单种子重排（sort_order/icon），补齐 17/18，role_menu 显式 id（雪花表无自增） |
| `backend/sql/V1.21__menu_sort_icons_and_my_meetings_fix.sql`（新增） | 存量环境菜单排序/图标/缺失菜单幂等修复 |
| `AuthGlobalFilter`（mrb-gateway） | 白名单移除 `/api/auth/register`、`/api/uc/user/register` |
| `UserController` / `UserService` / `UserServiceImpl`（mrb-user） | 下线注册接口（方法+DTO+相关测试删除） |
| `RegisterDTO`（删除） | 注册请求 DTO 下线 |
| `.harness/wiki/接口协议.md` | 移除注册接口与匿名白名单说明 |
| `frontend/index.html` / `public/favicon.svg`（新增） | 页面标题“会议室预约系统” + 品牌 favicon |
| `frontend/src/router/index.ts` | `afterEach` 按路由 meta.title 更新 `document.title` |
| `frontend/src/views/admin/MenuManage.vue` | 图标下拉补充 FolderOpened |

## 业务影响范围

- 用户菜单导航（普通用户 + 管理员）、登录/注册入口、浏览器标签页展示。
- 无 DB 表结构变更、无 MQ/缓存变更；`platform_menu` / `platform_role_menu` 为数据级调整。

## 冲突与风险

- **存量环境与全新环境一致化**：旧环境通过 V1.21 幂等迁移；全新环境以 init.sql 为准。
- **历史迁移不回溯**：V1.3/V1.8/V1.9/V1.13/V1.15 等历史 SQL 保持原样，避免重复执行产生歧义。
- **注册接口下线影响**：无前端入口依赖；管理员建号走 `POST /api/uc/user/admin/create`，不受影响。
- **角色菜单 id 生成**：V1.21 使用 `menu_id*100 + role_id` 作为关联 id，仅对缺失行生效，不与既有 id 冲突。
