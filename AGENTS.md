# 会议室预约系统 — AI 行动手册

> 本文档是 AI 执行任务的**唯一入口**。接到任何任务后，按以下协议执行。

---

## 一、红线守卫 (不可违反，任何代码改动前必须自检)

| # | 红线 | 违规示例 | 规范出处 |
|---|------|----------|----------|
| 1 | 价格字段必须使用 **BigDecimal**（元为单位，保留两位小数） | `private Long priceCents;` | [代码规范 §2.4](.harness/rules/代码规范.md) |
| 2 | Redis Key 前缀必须为 **`mrb:`** | `redisTemplate.opsForValue().set("user:123", ...)` | [代码规范](.harness/rules/代码规范.md) |
| 3 | RocketMQ 消费者必须**幂等** | Consumer 无去重逻辑 | [代码规范 §2.4](.harness/rules/代码规范.md) |
| 4 | 异常必须走 **BusinessException** 体系 | `throw new RuntimeException(...)` | [代码规范 §2.4](.harness/rules/代码规范.md) |
| 5 | Controller 必须**构造器注入** | `@Autowired private XxxService xxx;` | [代码规范 §2.2](.harness/rules/代码规范.md) |
| 6 | @Transactional 必须声明 **rollbackFor** | `@Transactional` 无 rollbackFor | [代码规范 §2.5](.harness/rules/代码规范.md) |
| 7 | Vue 3 必须使用 **`<script setup>`** | `export default { ... }` Options API | [代码规范 §3.2](.harness/rules/代码规范.md) |
| 8 | API 响应必须遵循 **`{code, message, data}`** | 直接返回实体对象 | [代码规范](.harness/rules/代码规范.md) |

**编码前自检**：逐条对照红线，确认即将编写的代码无违规。

---

## 二、任务执行协议

接到任务后，按以下阶段顺序执行。每个阶段必须完成才能进入下一阶段。

### 阶段 1：知识加载（必做）

读取顺序：**规范优先 → 按需加载 wiki**

| 优先级 | 必读 | 读取时机 |
|--------|------|----------|
| 🔴 强制 | `.harness/rules/代码规范.md` | 每次编码任务 |
| 🔴 强制 | `.harness/rules/工程结构.md` | 每次编码任务 |
| 🟡 按需 | `.harness/wiki/业务模型.md` | 涉及业务流程变更时 |
| 🟡 按需 | `.harness/wiki/数据模型.md` | 涉及数据库变更时 |
| 🟡 按需 | `.harness/wiki/接口协议.md` | 涉及 API 新增/修改时 |
| 🟡 按需 | `.harness/wiki/领域术语.md` | 需要统一术语时 |

> **约束**：单次任务按需加载 wiki 不超过 3 个，避免上下文膨胀。

### 阶段 2：需求分析

激活技能：[request-analysis](.harness/skills/request-analysis/SKILL.md)

1. 解析需求，提取核心功能点与验收标准
2. 识别受影响的业务模块（映射到领域模型）
3. 评估技术变更范围（DB / API / 缓存 / MQ / 前端）
4. 检测与现有功能的冲突
5. 产出 `.harness/changes/{feat-name}/summary.md`

### 阶段 3：编码实现

激活技能：[coding-skill](.harness/skills/coding-skill/SKILL.md)

按 Model → Repository → Service → Controller → 前端 的分层顺序实现：

| 步骤 | 后端 | 前端 |
|------|------|------|
| 1 | Entity / DTO / VO 定义 | TypeScript 类型定义 |
| 2 | Mapper 接口 | API 调用函数 |
| 3 | Service 接口 + 实现 | Pinia Store |
| 4 | MQ Producer / Consumer（如需） | 组件实现（`<script setup>`） |
| 5 | Controller | 页面路由注册 |
| 6 | 单元测试 | — |

**编码约束**：
- 遵循 [工程结构规范](.harness/rules/工程结构.md) 的四层架构与依赖方向
- 每个子任务完成后提交代码，提交信息遵循 [Conventional Commits](.harness/rules/开发流程规范.md)
- **任务完成后必须提交代码**，不得遗漏提交

### 阶段 4：代码评审

激活技能：[coded-review](.harness/skills/coded-review/SKILL.md) / [expert-reviewer](.harness/skills/expert-reviewer/SKILL.md)

- 常规代码：使用 coded-review
- 核心领域模型 / 架构方案：使用 expert-reviewer
- 发现问题 → 修复 → 重新评审

### 阶段 5：测试验证

激活技能：[unit-test-ci](.harness/skills/unit-test-ci/SKILL.md)

- 确保单元测试通过
- CI 门禁检查通过

---

## 三、变更追踪规范

每次功能变更必须在 `.harness/changes/{feat-name}/` 下产出：

| 文件 | 必须 | 说明 |
|------|------|------|
| `summary.md` | ✅ | 需求摘要 + 技术变更清单 + 冲突与风险 |
| `design.md` | 复杂功能必须 | 技术方案（含接口设计、数据模型变更、架构决策） |

**命名约定**：`{seq}-{feat-name}`，序号 3 位递增 + kebab-case 功能名。
- 序号从 `001` 起递增，新建时查看 `.harness/changes/` 下已有最大序号 +1
- 示例：`001-user-auth`、`012-reservation-approval`、`015-bug-ui-fixes`
- **效果**：文件系统按名称排序即按时间顺序，最新变更总在末尾

---

## 四、完成校验清单

任务完成前，逐项确认：

- [ ] **红线零违规**：8 条红线全部通过
- [ ] **规范合规**：代码符合 `.harness/rules/` 下所有规范
- [ ] **分层正确**：依赖方向 Controller → Service → Repository → Model，无反向依赖
- [ ] **变更已追踪**：`.harness/changes/{feat-name}/summary.md` 已产出
- [ ] **测试通过**：单元测试 + CI 门禁通过
- [ ] **代码已提交**：所有变更已 git commit，提交信息遵循 Conventional Commits 格式

---

## 五、知识库索引

### 规则（强制遵守）

| 文件 | 核心内容 | 何时读取 |
|------|----------|----------|
| [.harness/rules/代码规范.md](.harness/rules/代码规范.md) | 命名、异常、事务、前端规范 | 每次编码 |
| [.harness/rules/工程结构.md](.harness/rules/工程结构.md) | 前后端目录结构、四层架构 | 涉及新增文件/模块 |
| [.harness/rules/开发流程规范.md](.harness/rules/开发流程规范.md) | 分支管理、提交规范、评审流程 | 涉及 Git 操作 |

### 技能（按阶段激活）

| 文件 | 触发时机 |
|------|----------|
| [.harness/skills/request-analysis/SKILL.md](.harness/skills/request-analysis/SKILL.md) | 新功能 / 变更请求 |
| [.harness/skills/coding-skill/SKILL.md](.harness/skills/coding-skill/SKILL.md) | 编码实现 |
| [.harness/skills/coded-review/SKILL.md](.harness/skills/coded-review/SKILL.md) | 代码评审 |
| [.harness/skills/expert-reviewer/SKILL.md](.harness/skills/expert-reviewer/SKILL.md) | 核心领域 / 架构评审 |
| [.harness/skills/unit-test-ci/SKILL.md](.harness/skills/unit-test-ci/SKILL.md) | 测试与 CI |
| [.harness/skills/deploy-verify/SKILL.md](.harness/skills/deploy-verify/SKILL.md) | 部署验证 |

### 领域知识（按需加载）

| 文件 | 核心内容 | 何时读取 |
|------|----------|----------|
| [.harness/wiki/业务模型.md](.harness/wiki/业务模型.md) | 子域划分、核心业务流程、领域服务 | 涉及业务流程变更 |
| [.harness/wiki/数据模型.md](.harness/wiki/数据模型.md) | 数据库表结构、字段定义 | 涉及 DB 变更 |
| [.harness/wiki/接口协议.md](.harness/wiki/接口协议.md) | API 接口协议定义 | 涉及 API 新增/修改 |
| [.harness/wiki/领域术语.md](.harness/wiki/领域术语.md) | 统一术语表、状态定义、命名约定 | 需要统一术语 |

### Agent 定义

| 文件 | 说明 |
|------|------|
| [.harness/agents/owner.md](.harness/agents/owner.md) | Owner Agent 角色定义、决策原则、工作流程 |