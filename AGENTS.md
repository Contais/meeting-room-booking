# 会议室预约系统 AI 编码约束

## 技术栈

| 层级   | 技术                                       |
| :----- | :----------------------------------------- |
| 前端   | Vue 3 + TypeScript + Pinia + Vite + Element Plus |
| 后端   | Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Redis 7.x + RocketMQ 5.x |

## 红线 (不可违反)

1.  **价格字段必须使用 BigDecimal (元为单位,保留两位小数)** 🔴 - 禁止使用 Float/Double/Integer/Long 表示金额。
2.  **Redis Key 前缀必须为 `mrb:`** 🔴 - 禁止使用无名或通用前缀。
3.  **RocketMQ 消费者必须幂等** 🔴 - 每个消费者都必须处理重复消息。
4.  **异常处理必须使用 BusinessException 体系** 🔴 - 禁止原始异常泄露到 API 层。
5.  **Controller 必须使用构造器注入** 🔴 - 禁止使用 @Autowired 字段注入。
6.  **Service @Transactional 必须声明 rollbackFor** 🔴 - 禁止异常时静默提交。
7.  **Vue 3 必须使用 `<script setup>` 语法** 🔴 - 禁止使用 Options API。
8.  **API 响应必须遵循统一格式** 🔴 - `{code, message, data}` 结构。

## 文件索引

| 文件                                 | 用途                     |
| :----------------------------------- | :----------------------- |
| `.harness/agents/owner.md`           | 应用 Owner Agent 定义    |
| `.harness/rules/工程结构.md`         | 项目目录结构规范         |
| `.harness/rules/编码规范.md`         | 编码标准与约定           |
| `.harness/rules/开发流程规范.md`     | 开发流水线与流程         |
| `.harness/skills/request-analysis/SKILL.md` | 需求分析技能             |
| `.harness/skills/coding-skill/SKILL.md`     | 编码实现技能             |
| `.harness/skills/expert-reviewer/SKILL.md`  | 专家评审技能             |
| `.harness/skills/unit-test-write/SKILL.md`  | 单元测试编写技能         |
| `.harness/skills/unit-test-ci/SKILL.md`     | CI 与质量门禁技能        |
| `.harness/skills/deploy-verify/SKILL.md`    | 部署验证技能             |
| `.harness/wiki/业务模型.md`          | 业务模型与实体关系       |
| `.harness/wiki/接口协议.md`          | API 接口协议定义         |
| `.harness/wiki/数据模型.md`          | 数据库 Schema 与表定义   |
| `.harness/changes/`                  | 变更追踪目录             |