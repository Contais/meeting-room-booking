# 单元测试 CI 技能 (Unit Test CI)

## 概述
在持续集成阶段强制执行单元测试覆盖率门槛和质量门禁，拒绝低质量代码合入主分支。

## 触发条件
当开发者提交代码并推送到远程仓库（Push/PR）时，CI 流水线自动执行此技能。

## 执行标准 (盖茨比)

### 1. 覆盖率门槛 (Coverage Threshold)
- **整体覆盖率**：必须 >= 80%。
- **核心业务模块**：必须 >= 90%（如订单、支付、库存模块）。
- **变更文件覆盖率**：针对本次 PR 修改的文件，其新增代码的覆盖率必须 >= 70%。

### 2. 测试执行规范
- **Mock 策略**：
  - 所有外部依赖（Database, Redis, FeignClient）必须使用 `@MockBean` 或 Mockito 进行 Mock。
  - 禁止在单元测试中启动真实的 Web 服务器（除非使用 @SpringBootTest）。
- **断言严格性**：
  - 禁止使用 `assertNotNull(result)` 这种模糊断言。
  - 必须使用 `assertEquals(expected, actual)`，`assertTrue(condition)` 并附带明确的错误信息。
  - 推荐使用 AssertJ 流式断言（如 `assertThat(order.getStatus()).isEqualTo(PENDING_PAYMENT)`）。

### 3. 坏味道检查 (Anti-Patterns)
- **无断言测试**：禁止只写了 `@Test` 方法但没有任何 `assert` 语句的测试用例（被视为无效测试）。
- **忽略测试**：禁止在生产分支中使用 `@Ignore` 注解。
- **过长测试**：单个测试方法长度不得超过 20 行，逻辑必须单一。

### 4. 测试报告输出
- 生成 JaCoCo 覆盖率报告，并高亮显示未覆盖的代码行。
- 如果未通过覆盖率门槛，CI 构建失败，并提示开发者补充测试用例。