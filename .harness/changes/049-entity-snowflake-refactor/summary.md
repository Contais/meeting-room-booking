# 实体与数据库统一规范重构 - BaseEntity + 雪花主键

> 将现有代码对齐 `.harness/changes/048-database-entity-standard` 固化的数据库规范：
> 1. 所有表/实体统一包含 `id`、`create_time`、`update_time`、`deleted`，公共字段抽取 `BaseEntity` 父类；
> 2. 所有 `id` 统一为 `Long` + MyBatis-Plus 内置雪花算法（`IdType.ASSIGN_ID`），移除数据库自增。

## 需求摘要

1. 抽取 `BaseEntity`（mrb-common），统一声明 `id`（`@TableId(type = IdType.ASSIGN_ID)`）、`createTime`、`updateTime`、`deleted`。
2. 14 个实体全部改为继承 `BaseEntity`，删除各自重复的基础字段与 `IdType.AUTO`。
3. 全局 `id-type: auto` 改为 `assign_id`，与新实体注解一致。
4. DDL 对齐：移除 `AUTO_INCREMENT`，`role_menu` 补齐缺失的 `update_time` / `deleted`。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `mrb-common/.../common/model/BaseEntity.java` | 新增：统一主键（雪花）+ 审计字段（自动填充 + 逻辑删除） |
| `mrb-meeting / mrb-user / mrb-platform` 三个服务共 14 个 Entity | 继承 `BaseEntity`，移除 `IdType.AUTO` 与重复字段 |
| `mrb-meeting / mrb-user / mrb-platform` application.yml、mrb-user test yml、config-example（2 个）、backend/config（2 个，未跟踪） | `id-type: auto` → `assign_id` |
| `backend/sql/init.sql` | 8 张表移除 `AUTO_INCREMENT`；`role_menu` 补齐 `update_time` / `deleted` |
| `backend/sql/V1.18__entity_snowflake_standard.sql` | 新增：已部署环境增量脚本（补字段 + 去自增，含旧 notification 表存在性守卫） |
| `mrb-user-service/src/test/resources/schema.sql` | H2 测试表主键去自增，与正式 DDL 保持一致 |

## 业务影响范围

- 纯主键生成策略与实体结构重构，无接口、无前端、无业务行为变更。
- 主键由自增小整数变为 19 位雪花 Long；已有 Jackson `Long -> String` 序列化，前端不会发生精度丢失。
- 现有数据的主键值保持不变（`MODIFY COLUMN` 仅移除 `AUTO_INCREMENT` 属性，不改数据）。

## 冲突与风险

- **历史迁移文件不改动**：V1.1~V1.17 及 `V1.13__reset_data_and_init.sql` 中的 `AUTO_INCREMENT` 属历史产物，保留原样；全新环境以 init.sql 为准，已部署环境需执行 V1.18。
- **`init.sql` 与迁移文件不同步（存量问题）**：init.sql 缺少 equipment / room_equipment / reservation_attendee / mrb_platform 系列表，本次仅按规范修订其中已有表，未补全缺失表，建议后续单独治理。
- **测试**：`UserServiceImplTest` 等 mock 用例设置 id 为 1L 不受影响；H2 测试 schema 已同步去自增。
- 未改动 `mrb-auth`（无自有实体与表）。

## 验收标准

- `rg "IdType.AUTO" backend --glob '*.java'` 无结果。
- 全部实体继承 `BaseEntity`，无重复声明 `id` / `createTime` / `updateTime` / `deleted`。
- 主配置与示例配置中 `id-type` 均为 `assign_id`。
- 后端各模块编译通过，既有单元测试通过。
