# 后端工程结构重构：嵌套聚合器 + api 契约模块拆分

> 将各服务的跨服务契约（Feign 客户端、跨服务 DTO/枚举/常量、通知发送外观）抽离到独立的 api 模块，
> 并将每个服务转为「聚合器 + api + service 子模块」的嵌套结构，消除 Feign 客户端重复定义与 mrb-common 归属混乱。

## 需求摘要

1. **消除 Feign 客户端重复**：`FileFeignClient` 在 mrb-meeting / mrb-user 各有一份逐字重复；`UserFeignClient` 在 mrb-auth / mrb-meeting 各有一份（方法集重叠）。
2. **消除 mrb-common 归属混乱**：`NotificationSendDTO`/`NotificationMessage`/`MqConstant` 实属平台域、`ReservationStatusEnum`/`AttendeeStatusEnum` 实属会议室域、`AuthUserDTO` 实属用户域，却堆在 mrb-common。
3. **彻底消除跨服务通知发送重复**：`NotificationSender`+`NotificationProducer` 原只在 mrb-meeting，其他服务需发送通知时无处复用，易重复实现降级逻辑。
4. **建立 api 契约边界 + 嵌套聚合器结构**：跨服务契约抽离为独立 jar，服务转为聚合器（pom）下嵌 `mrb-XXX-api`（契约）与 `mrb-XXX-service`（实现）两个子模块，依赖方向清晰。

## 最终模块结构

```
backend/
├── mrb-common/                         # 通用基础（保留跨域共享内容）
├── mrb-gateway/                        # 网关（不拆分，单模块）
├── mrb-auth/                           # 聚合器（pom）
│   ├── mrb-auth-api/                   # 契约（结构占位：auth 无对外 Feign 契约）
│   └── mrb-auth-service/               # 实现（原 mrb-auth/src 迁入）
├── mrb-meeting/                        # 聚合器（pom）
│   ├── mrb-meeting-api/                # 契约（跨服务枚举）
│   └── mrb-meeting-service/            # 实现（原 mrb-meeting/src 迁入）
├── mrb-platform/                       # 聚合器（pom）
│   ├── mrb-platform-api/               # 契约（通知 Feign/Producer、文件 Feign、MQ 常量、跨服务 DTO）
│   └── mrb-platform-service/           # 实现（原 mrb-platform/src 迁入）
└── mrb-user/                           # 聚合器（pom）
    ├── mrb-user-api/                   # 契约（User/Department Feign、AuthUserDTO 等）
    └── mrb-user-service/               # 实现（原 mrb-user/src 迁入）
```

根 `backend/pom.xml` <modules> 仅注册：`mrb-common`、`mrb-gateway`、4 个服务聚合器。子模块构建顺序由各聚合器内部 `<modules>` 声明（api 先于 service）。

## 技术变更清单

### 一、4 个服务转为嵌套聚合器结构

| 服务 | 聚合器 pom | api 子模块 | service 子模块 |
|------|-----------|-----------|---------------|
| `mrb-auth` | `mrb-auth/pom.xml`（packaging=pom） | `mrb-auth-api`（结构占位） | `mrb-auth-service`（原 src 经 `git mv` 迁入，含原依赖 + spring-boot-maven-plugin） |
| `mrb-meeting` | `mrb-meeting/pom.xml`（packaging=pom） | `mrb-meeting-api` | `mrb-meeting-service` |
| `mrb-platform` | `mrb-platform/pom.xml`（packaging=pom） | `mrb-platform-api` | `mrb-platform-service` |
| `mrb-user` | `mrb-user/pom.xml`（packaging=pom） | `mrb-user-api` | `mrb-user-service` |

- 各聚合器 pom：parent 为 `mrb-backend`，`<packaging>pom</packaging>`，`<modules>` 声明 api + service。
- 各 service pom：parent 为对应聚合器，包含原服务全部依赖与 `spring-boot-maven-plugin`。
- 各 api pom：parent 为对应聚合器，依赖 `mrb-common` + 契约所需（openfeign/loadbalancer/rocketmq 视需要）。
- 原 `src` 目录树通过 `git mv` 迁入 `mrb-XXX/mrb-XXX-service/src`，保留 git 历史。

### 二、新增 4 个 api 契约模块内容

| 模块 | 包根 | 内容 |
|------|------|------|
| `mrb-user-api` | `com.meetinghub.user.api` | `UserFeignClient`（合并 auth/meeting 两处）、`DepartmentFeignClient`、`AuthUserDTO`、`UserBriefDTO`、`DepartmentBriefDTO` |
| `mrb-platform-api` | `com.meetinghub.platform.api` | `NotificationFeignClient`、`FileFeignClient`（合并 meeting/user 两处）、`NotificationSendDTO`、`NotificationMessage`、`MqConstant`、`NotificationSender`、`NotificationProducer` |
| `mrb-meeting-api` | `com.meetinghub.meeting.api` | `ReservationStatusEnum`、`AttendeeStatusEnum` |
| `mrb-auth-api` | `com.meetinghub.auth.api` | （空，结构占位：auth 无对外 Feign 契约） |

各 api 模块 pom 依赖：`mrb-common` + 契约所需（user/platform-api 含 openfeign+loadbalancer；platform-api 含 rocketmq；meeting-api/auth-api 仅 lombok）。

### 三、删除重复定义 / 归属迁移

| 删除项 | 去向 |
|------|------|
| `mrb-auth/feign/UserFeignClient.java` | 合并入 `mrb-user-api` |
| `mrb-meeting/feign/UserFeignClient.java` + `DepartmentFeignClient.java` + `feign/dto/*` | 迁入 `mrb-user-api` |
| `mrb-meeting/feign/FileFeignClient.java` + `NotificationFeignClient.java` | 迁入 `mrb-platform-api` |
| `mrb-user/feign/FileFeignClient.java` | 删除（用 `mrb-platform-api` 的） |
| `mrb-meeting/mq/producer/NotificationSender.java` + `NotificationProducer.java` | 迁入 `mrb-platform-api` |
| `mrb-common/model/dto/{AuthUserDTO,NotificationSendDTO,NotificationMessage}.java` | 分别迁入 `mrb-user-api` / `mrb-platform-api` |
| `mrb-common/constant/MqConstant.java` | 迁入 `mrb-platform-api` |
| `mrb-common/enums/{ReservationStatusEnum,AttendeeStatusEnum}.java` | 迁入 `mrb-meeting-api` |

### 四、service 模块增补 api 依赖

| service | 新增依赖 |
|---------|---------|
| `mrb-auth-service` | `mrb-user-api`（UserFeignClient + AuthUserDTO） |
| `mrb-meeting-service` | `mrb-meeting-api` + `mrb-user-api` + `mrb-platform-api` |
| `mrb-platform-service` | `mrb-platform-api`（NotificationConsumer 用 MqConstant/NotificationMessage） |
| `mrb-user-service` | `mrb-platform-api`（FileFeignClient）+ `mrb-user-api`（UserController 实现 getUserForAuth 返回 AuthUserDTO） |

### 五、引用方 import 批量更新

对 17 个服务文件（meeting 的 service/tools/schedule/vo、platform 的 controller/service/mq、user 的 controller/service、auth 的 service + 测试）按 16 处包路径映射更新 import，均为确定性字符串替换，git 识别为 rename（保留历史）。

## 关键架构决策

1. **采用嵌套聚合器结构**：每个服务转为聚合器（pom），下嵌 `mrb-XXX-api`（契约 jar）与 `mrb-XXX-service`（实现）两个子模块。相比平铺结构，聚合器将「一个服务的契约与实现」在文件系统中归拢到同一目录树下，模块边界与归属一目了然，符合「高内聚」原则。
2. **mrb-auth-api 作为结构占位保留**：auth 当前无任何跨服务 Feign 契约可暴露（无服务通过 Feign 调用 mrb-auth；网关用自有 JWT 过滤器，不走 Feign）。但为保持 4 个服务结构对称（均有 api + service 子模块），保留空 api 模块作为占位，未来若 auth 需暴露契约可直接填充。
3. **RedisKeyConstant 保留在 mrb-common**：其字段跨域使用（`USER_TOKEN`→auth、`SCHEDULE_*`→meeting、`MQ_DEDUP`/`PREFIX`→platform），集中管理更利于红线 #2（`mrb:` 前缀统一管控），拆分反而分散前缀管控、增加维护成本。`DateTimePatternConstant` 同理留 common。

## 组件扫描与运行时说明

- 所有 Application 类均 `@SpringBootApplication(scanBasePackages = "com.meetinghub")`（组件扫描覆盖 com.meetinghub，含 api 模块的 `@Component`）。
- **`@EnableFeignClients(basePackages = "com.meetinghub")` 必须显式指定**：`@EnableFeignClients` 默认只扫描 Application 类所在包（如 `com.meetinghub.meeting`），不覆盖 api 模块的 `com.meetinghub.user.api.feign`/`com.meetinghub.platform.api.feign`。重构后 Feign 客户端迁出服务自身包，必须显式指定 basePackages，否则启动报 `required a bean of type 'XxxFeignClient' that could not be found`。
- `NotificationSender` 作为 `@Component` 落在 platform-api：依赖 platform-api 的服务（meeting/user/platform）均可装配。`NotificationProducer` 加 `@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")`，仅在配置了 RocketMQ 的服务（meeting/platform）中创建；未配置 RocketMQ 的服务（如 user）不创建 Producer，`NotificationSender` 通过 `ObjectProvider<NotificationProducer>` 弱依赖，缺失时直接走 Feign 降级，不启动报错。
- 运行时配置（application.yml/bootstrap.yml）随 src 迁入 service 子模块，路径变化不影响 Nacos 配置加载（配置中心按 dataId 拉取，与本地路径无关）。

## 冲突与风险

- **行为零变更**：纯结构重构，无业务逻辑改动。Feign 客户端合并后方法集为两者并集（getUserForAuth 在 auth/meeting 两处签名一致），调用方行为不变。
- **UserFeignClient bean 合并**：原 auth-service 与 meeting-service 各持一个 `@FeignClient(name="mrb-user")`（不同 JVM，无冲突）；合并为 user-api 单一客户端后，各服务通过 classpath 引用，仍各自单实例，无 bean 冲突。DepartmentFeignClient 保留 `contextId="departmentFeignClient"` 与 UserFeignClient 共存（沿用原模式）。
- **Feign 自调用**：platform-service 装配的 NotificationSender 降级路径会 Feign 调用自身（mrb-platform），属回环但不致命（仅在 MQ 失败时触发，经负载均衡回自身端点）。
- **mrb-common 瘦身**：移除 6 个归属混乱类后，`common/model/dto/` 目录清空（其余 common 内容：Result、BusinessException、ErrorCode、UserContext、拦截器、RequiresRole 注解、配置类、CommonConstant、DateTimePatternConstant、RedisKeyConstant、其余 enums 均保留）。
- **本地配置保护**：`.gitignore` 新增 `*-local.yml`，避免本地开发凭证（含 Nacos/COS/DB 密码）误提交。

## 验证点

1. **全量编译**：`cd backend && mvn -q clean compile -DskipTests` 通过
2. **测试编译**：`mvn -q test-compile -DskipTests` 通过（auth 测试引用迁移后的 UserFeignClient/AuthUserDTO）
3. **运行时 Feign 装配**：4 个 Application 类的 `@EnableFeignClients` 显式指定 `basePackages = "com.meetinghub"`，确保 api 模块的 Feign 客户端被扫描装配（修复启动报 `UserFeignClient bean not found`）
4. **无残留旧引用**：grep 确认服务模块中无 `com.meetinghub.common.{model.dto.AuthUserDTO,model.dto.NotificationSendDTO,model.dto.NotificationMessage,constant.MqConstant,enums.ReservationStatusEnum,enums.AttendeeStatusEnum}` 及 `meeting.feign./meeting.mq.producer./auth.feign./user.feign.` 残留
5. **提交分阶段**：
   - Stage1：3 个 api 契约模块骨架（`8594bf2`）
   - Stage2：跨服务契约迁入 api 模块，消除 Feign 客户端重复定义（`c4bb076`）
   - Stage3：服务转为嵌套聚合器结构（`dd560e4`）—— `git mv` 迁移 src 树、重写 12 个 pom、根 pom 调整 modules
   - Stage4：修复 `@EnableFeignClients` basePackages 扫描范围（运行时 Feign 装配）
