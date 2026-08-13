# V1.13 重置/种子数据合并进 init.sql

## 需求摘要

将 `V1.13__reset_data_and_init.sql` 的多层级部门（22 个）与 200 名员工种子数据合并进 `init.sql`，
并适配雪花主键与 053 的表名/RBAC 迁移结果，作为全新环境初始化 + 数据重置的统一入口。

## 雪花算法对 V1.13 的影响（本变更解决点）

1. `ALTER TABLE ... AUTO_INCREMENT = 1` 已无意义（主键不再自增），合并时删除。
2. 无显式 `id` 的 INSERT（role / role_menu / 员工）在无自增列下会直接失败，合并时全部改为显式 id：
   - 员工 id 从 3 开始（admin=1、test=2 已存在）。
3. `role_menu` 关联列由 `role`（角色编码）改为 `role_id`（角色ID），种子按 role_id 写入。
4. 表名/归属同步 053 新规范：`user`→`uc_user`、`department`→`uc_department`，
   role/role_menu 位于 `mrb_platform`（`platform_role`/`platform_role_menu`）。
5. 修正 V1.13 遗留的 `department.sort` 列名（实际为 `sort_order`）。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `backend/sql/init.sql` | 新增 22 个部门种子 + 存储过程生成 200 名员工（显式 id）；角色/角色菜单沿用已有 role_id 种子 |
| `V1.13__reset_data_and_init.sql` | 保留为历史迁移，不改动；数据重置以 init.sql 为准 |

## 数据重置方法

对已存在环境重新初始化：先 `DROP DATABASE`（mrb_user / mrb_auth / mrb_meeting / mrb_platform），再执行 `init.sql`。

## 验收标准

- 全新环境执行 init.sql 后：uc_user 202 条（admin/test + 200 员工）、uc_department 22 条、platform_menu 16 条、platform_role 2 条、platform_role_menu 17 条。
- 员工手机号/邮箱分布符合 80/8/8/4 比例。
