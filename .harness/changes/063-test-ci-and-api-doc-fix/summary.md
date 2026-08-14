# 063-test-ci-and-api-doc-fix 需求分析摘要

## 需求描述

系统查漏补缺第二阶段：

1. 修复 `接口协议.md` 与代码不一致的漂移点（菜单/角色已从 mrb-user 迁到 mrb-platform、文件内部预签名路径、路由归属表）。
2. 搭建单元测试/CI 底座：引入 JaCoCo 覆盖率报告与可开关的门禁，新增 GitHub Actions 工作流，并补齐核心域首轮单元测试。
3. 性能测试（JMeter）先规划，脚本与执行放后续版本。

## 验收标准

- [ ] `接口协议.md` 中菜单/角色路径统一为 `/api/platform/**`，不再出现 `/api/uc/menu`、`/api/uc/admin/role` 错误前缀
- [ ] 文件内部预签名路径修正为 `/platform/internal/file/presigned-urls`
- [ ] 网关路由表「菜单、角色」归属 mrb-platform；「用户、部门」归属 mrb-user
- [ ] 根 pom 配置 JaCoCo：`mvn verify` 生成各模块覆盖率报告，门禁默认不阻断、可 `-Djacoco.haltOnFailure=true` 开启
- [ ] 新增 `.github/workflows/ci.yml`：push/PR 触发，JDK 17 + Maven `verify`，并上传 JaCoCo 报告
- [ ] 新增 `ReservationServiceImplTest`、`MeetingRoomServiceImplTest` 并全部通过

## 技术变更清单

| 变更 | 说明 |
|------|------|
| `.harness/wiki/接口协议.md` | 菜单/角色归属与路径修正、内部文件路径修正、chat stream 标注修正 |
| `backend/pom.xml` | 引入 jacoco-maven-plugin（prepare-agent/report/check，haltOnFailure 可配置） |
| `.github/workflows/ci.yml`（新增） | GitHub Actions 构建、测试、上传覆盖率报告 |
| `ReservationServiceImplTest`（新增） | 预约创建/冲突/规则/审批/取消 10 个用例 |
| `MeetingRoomServiceImplTest`（新增） | 会议室默认规则/自定义规则/启停/详情 4 个用例 |
| `docs/PERFORMANCE_TEST_PLAN.md`（新增） | 后续版本 JMeter 压测规划 |

## 业务影响范围

- 文档与测试底座，无业务逻辑改动、无 DB 变更。

## 冲突与风险

- **覆盖率门禁暂不强制**：当前各模块覆盖率远低于 80%，直接开启会阻断所有构建；已用 `jacoco.haltOnFailure` 默认 false 保留报告，待覆盖率达标后由 CI 传 `-Djacoco.haltOnFailure=true` 开启。
- **ServiceImpl 单测方式**：业务 Service 继承 MyBatis-Plus `ServiceImpl`，测试中通过反射注入 `baseMapper` 为 Repository mock，避免真实数据库；该模式已在新增用例中固化。
- **性能测试依赖 #4**：预约创建并发正确性压测依赖「冲突检测原子化」修复，未修复前仅用于缺陷复现。
