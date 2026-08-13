# 表名服务前缀统一 + RBAC 集群迁移

> 在 049 雪花主键与 052 前端精度修复基础上，统一数据库表名为「服务前缀 + 实体 snake_case（单数）」，
> 并将 menu / role / role_menu 从 mrb-user 迁至 mrb-platform，明确各服务职责边界。

## 需求摘要

1. 所有表统一服务前缀：`uc_` / `meeting_` / `platform_`。
2. menu、role、role_menu 迁入 mrb-platform，`role_menu.role`（编码）统一为 `role_id`。
3. 删除 mrb_user 遗留的 notification 表。

## 技术变更清单

| 文件/目录 | 变更 |
|-----------|------|
| 14 个 `@TableName` 实体 + 5 个 Mapper XML | 表名改为带服务前缀；RBAC 实体随代码搬移 |
| mrb-user-service → mrb-platform-service（19 文件） | Menu/Role/RoleMenu 的 controller/entity/repository/service/dto/vo 整体迁移 |
| `RoleMenu` 实体 + 相关 Service | `role` → `roleId`，删除 `saveRoleMenus` 死代码 |
| `frontend/src/api/menu.ts` / `role.ts` | 路径 `/api/uc/*` → `/api/platform/*` |
| `backend/sql/V1.19__apply_service_prefix.sql` | 同库 RENAME + DROP 遗留 notification |
| `backend/sql/V1.20__migrate_rbac_to_platform.sql` | RBAC 跨库数据迁移 + role_id 化 |
| `backend/sql/init.sql` / H2 `schema.sql` / wiki / 代码规范 | 同步新表名与约定 |

## 冲突与风险

- 跨库数据迁移 + 代码模块搬移 + 网关/前端路径调整，属于中等规模重构，需按迁移窗口执行并做备份。
- `role_menu.role` → `role_id` 需在 V1.20 中按 `role.role_code` 关联转换，避免丢关联。
- 迁移期间菜单/角色应暂停写操作；`user.role` 保留编码字符串，登录链路不受影响。

## 验收标准

- 表名全部符合「服务前缀 + 实体 snake_case」。
- RBAC 相关代码仅存在于 mrb-platform，mrb-user 无残留引用。
- `role_menu` 使用 `role_id`，无 `role` 编码列残留。
- 后端编译/单测、前端 build 通过；迁移后真实库冒烟通过。
