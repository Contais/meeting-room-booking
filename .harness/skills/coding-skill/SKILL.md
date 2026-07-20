# 编码技能

## 概述

按照标准流程实现功能编码，确保代码质量与架构合规，遵循 DDD 分层架构和 Clean Code 原则。

## 触发条件

当需要编写或修改项目代码时，自动激活此技能。

## 前置条件

- 需求分析已完成
- 技术方案已评审
- 任务已拆分并明确依赖

## 上下文准备

- 读取相关的规则文件 (按需加载，不超过 3 个)
- 读取相关的 wiki 文件 (按需加载，不超过 3 个)
- 确认当前任务涉及的模块和文件

## 后端编码步骤

### Step 1: Model (模型层设计)
- **定义 Entity**：对应数据库表结构，包含所有字段及 JPA/MyBatis-Plus 注解。
- **定义 DTO**：接收前端请求参数，使用 `@Valid` 进行参数校验。
- **定义 VO**：封装返回给前端的数据，剥离敏感信息。
- **检查**：金额字段必须为 `Integer`（分），命名规范需符合 Snake Case 或 Camel Case。

### Step 2: Repository (数据访问层)
- **创建 Mapper 接口**：继承 `BaseMapper<T>`，禁止手写基础的 CRUD SQL。
- **复杂查询编写**：使用 MyBatis-Plus 的 `Wrapper` 或自定义 XML，禁止使用原生 JDBC。
- **检查**：禁止硬编码 SQL 字段名，必须使用 LambdaQueryWrapper 防止字段变更导致出错。

### Step 3: Service (业务逻辑层)
- **定义 Service 接口**：遵循接口隔离原则，只暴露必要的方法。
- **实现 ServiceImpl**：编写具体的业务逻辑。
- **@Transactional 声明**：明确事务边界，查询方法不加事务，写操作必须指定 `rollbackFor`。
- **检查**：业务逻辑完整性（如库存不足时的抛出 `BusinessException`），异常处理是否捕获并转换。

### Step 4: MQ (消息队列集成)
- **定义消息体 Event 类**：使用 Lombok 简化 POJO，必须包含 `traceId`。
- **创建 Producer**：发送消息时需处理发送失败的重试逻辑。
- **创建 Consumer**：消费消息时必须实现幂等性校验（Redis 分布式锁防重）。

### Step 5: 单元测试 (Unit Test)
- **Controller 层**：使用 `@WebMvcTest` 测试接口连通性和参数校验。
- **Service 层**：使用 `@SpringBootTest` 模拟数据库环境，测试核心业务逻辑分支。