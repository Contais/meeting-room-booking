# 表名服务前缀统一 + RBAC 集群迁移 design

## 1. 背景与目标

在雪花主键（049）与 id 前端精度修复（052）之后，进一步统一数据库命名，并将 RBAC 领域（menu / role / role_menu）从用户中心迁到平台中心，使各服务职责边界更清晰：

- mrb-user 只保留账号（user）与组织（department）。
- mrb-platform 承载系统管理与基础能力：通知、字典、系统配置，以及迁入的菜单/角色/权限。

## 2. 命名规范（定稿）

1. 表名单数、snake_case，统一带服务前缀。
2. 服务前缀：`uc_`（用户中心）、`meeting_`（会议室）、`platform_`（平台）。
3. 关联表使用「完整左表名 + 右表名」。
4. 实体类名保持不变（Java 已按包隔离），仅 `@TableName`、Mapper、DDL 反映前缀。
5. 系统元数据表不再使用 `sys_`，统一归入 `platform_`。

## 3. 表名映射

| 库 | 旧表名 | 新表名 | 动作 |
|----|--------|--------|------|
| mrb_user | user | uc_user | RENAME |
| mrb_user | department | uc_department | RENAME |
| mrb_user | menu | platform_menu | 迁 mrb_platform |
| mrb_user | role | platform_role | 迁 mrb_platform |
| mrb_user | role_menu | platform_role_menu | 迁 mrb_platform + role_id 化 |
| mrb_user | notification | — | DROP（已迁平台） |
| mrb_meeting | meeting_room | meeting_room | 不动 |
| mrb_meeting | meeting_room_reservation | meeting_room_reservation | 不动 |
| mrb_meeting | equipment | meeting_equipment | RENAME |
| mrb_meeting | room_equipment | meeting_room_equipment | RENAME |
| mrb_meeting | reservation_attendee | meeting_room_reservation_attendee | RENAME |
| mrb_platform | notification | platform_notification | RENAME |
| mrb_platform | sys_dict | platform_dict | RENAME |
| mrb_platform | sys_dict_item | platform_dict_item | RENAME |
| mrb_platform | sys_config | platform_config | RENAME |

## 4. RBAC 集群代码搬移

将以下 19 个文件从 `mrb-user-service` 搬到 `mrb-platform-service`，包名由 `com.meetinghub.user.*` 改为 `com.meetinghub.platform.*`：

- controller：MenuController、RoleController
- entity：Menu、Role、RoleMenu
- repository：MenuRepository、RoleRepository、RoleMenuRepository
- service：MenuService、RoleService、impl/MenuServiceImpl、impl/RoleServiceImpl
- dto：MenuCreateDTO、MenuUpdateDTO、RoleCreateDTO、RoleUpdateDTO、RoleMenuAssignDTO（新增 `com.meetinghub.platform.model.dto` 包）
- vo：MenuVO、RoleVO

已确认 mrb-user 内部除上述文件外无对 Menu/Role/RoleMenu 的引用，集群自包含，可整体搬移。Menu/Role/RoleMenu 无 Mapper XML（走 BaseMapper），无需搬 XML。

## 5. role_menu 统一为 role_id

当前 `role_menu.role` 存角色编码（VARCHAR），而接口层 `RoleMenuAssignDTO` 传 roleId，内部再回查 role_code 存储，存在隐式转换与命名歧义。本次统一：

- 列：`role` VARCHAR → `role_id` BIGINT NOT NULL（指向 role.id）。
- 实体：`RoleMenu.role` → `RoleMenu.roleId`。
- 唯一索引：`uk_role_menu(role_id, menu_id)`。
- `MenuServiceImpl.listByRole(roleCode)`：先按 `role_code` 查 role.id，再按 `role_id` 查 role_menu。
- `assignMenus` / `getRoleMenuIds` / `deleteRole` 直接按 `role_id` 操作。
- 删除死代码：`MenuService.saveRoleMenus`、`MenuController /admin/role-menus`、前端 `api/menu.ts#saveRoleMenus`（已确认前端无引用）。

## 6. 网关与前端

- `frontend/src/api/menu.ts`：`/api/uc/menu/**` → `/api/platform/menu/**`。
- `frontend/src/api/role.ts`：`/api/uc/admin/role/**` → `/api/platform/admin/role/**`。
- 网关平台路由已覆盖 `/api/platform/**`，无需新增路由；`/api/uc/**` 继续服务 user/department。
- 用户管理（UserManage）的角色下拉改为前端直连 platform 的 `listAllRoles`，不新增后端 Feign。

## 7. 存量库迁移脚本

`V1.19__apply_service_prefix.sql`：
- RENAME：user→uc_user、department→uc_department、equipment→meeting_equipment、room_equipment→meeting_room_equipment、reservation_attendee→meeting_room_reservation_attendee、notification→platform_notification、sys_dict→platform_dict、sys_dict_item→platform_dict_item、sys_config→platform_config。
- DROP mrb_user.notification。

`V1.20__migrate_rbac_to_platform.sql`：
- 在 mrb_platform 建 `platform_menu`、`platform_role`、`platform_role_menu(role_id)`。
- `INSERT…SELECT` 迁移 menu / role。
- role_menu 通过 `JOIN role ON role.role_code = role_menu.role` 将 role 编码转为 role_id。
- 迁移验证通过后再 DROP mrb_user.menu / role / role_menu。

## 8. 同步项

- `backend/sql/init.sql`：全新环境按新表名与新归属重建（含 RBAC 表迁入 mrb_platform）。
- `mrb-user-service/src/test/resources/schema.sql`：user → uc_user。
- `.harness/wiki/数据模型.md`：库表结构按新归属与新表名更新。
- `.harness/rules/代码规范.md`：补充「表名 = 服务前缀 + 实体 snake_case（单数）」约定。

## 9. 验证

- 后端 `mvn -o` 编译通过；既有单测通过。
- 前端 `npm run build`（vue-tsc + vite）通过。
- 迁移后真实库冒烟：登录 → 侧边栏菜单；菜单/角色管理；用户管理角色下拉；会议室预约与参会人邀请链路。

## 10. 回滚与风险

- 同库 RENAME 可通过反向 RENAME 回滚；跨库迁移建议执行前对 mrb_user.menu/role/role_menu 做 mysqldump 或 RENAME `_bak` 备份，验证通过后再清理。
- 迁移窗口内避免对菜单/角色进行写操作，防止增量数据丢失。
- `user.role` 仍为角色编码字符串（供 JWT / auth 使用），与 role 表为逻辑关联，无外键，迁移不破坏登录链路。
