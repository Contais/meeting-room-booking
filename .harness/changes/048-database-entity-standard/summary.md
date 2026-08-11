# 数据库统一规范 - 基础字段与雪花主键策略

> 将后端数据库设计统一为以下规范，并固化到 `.harness` 知识库相关文件中：
> 1. 所有表设计必须包含 `id`、`create_time`、`update_time`、`deleted` 字段；公共字段可抽取父类 `BaseEntity`。
> 2. 所有 `id` 字段统一使用 `Long` 类型 + MyBatis-Plus 内置雪花算法（`@TableId(type = IdType.ASSIGN_ID)`）。

## 需求摘要

1. **基础字段强制**：所有业务表必须包含 `id`、`create_time`、`update_time`、`deleted` 四个字段，字段定义见下表。
2. **主键统一**：所有 `id` 使用 `Long` + MyBatis-Plus 雪花算法（`IdType.ASSIGN_ID`），禁止数据库自增。
3. **父类抽取**：公共字段重复时可抽取 `BaseEntity`，建议置于 mrb-common 模块。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `.harness/rules/代码规范.md` | 新增 §2.7 数据库与实体规范：基础字段、雪花主键、逻辑删除、时间自动填充 |
| `.harness/wiki/数据模型.md` | 新增「数据库统一规范（强制）」章节：基础字段定义表、主键策略、实体映射；原表结构章节顺延为 §2 |
| `.harness/rules/工程结构.md` | 补充实体基础字段收敛到 `BaseEntity`（mrb-common）及雪花主键约定 |

## 业务影响范围

- 仅涉及 `.harness` 知识库文档，无业务逻辑变更，不影响现有接口与前端。

## 冲突与风险

- **现状与规范不一致（需后续改造）**：当前 14 个实体均为 `@TableId(type = IdType.AUTO)`，DDL 使用 `AUTO_INCREMENT`，且无 `BaseEntity`，公共字段在各实体重复声明。
- 本次仅固化规范，未做代码/DDL 改造；后续对齐建议单独立项：新增 `BaseEntity` → 批量替换 `IdType.ASSIGN_ID` → 迁移 DDL 移除 `AUTO_INCREMENT` → 数据兼容验证。
- 未修改 `AGENTS.md` 红线表；如需将两条新规则升级为红线，可后续补充。

## 验收标准

- `.harness/rules/代码规范.md` 包含数据库基础字段与雪花主键规则。
- `.harness/wiki/数据模型.md` 包含统一字段定义表与主键策略。
- 后续新增表 DDL / 新实体必须遵循上述规范。
