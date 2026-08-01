# 后端工程结构重构：api 契约模块拆分

> 将各服务的跨服务契约（Feign 客户端、跨服务 DTO/枚举/常量、通知发送外观）抽离到独立的 api 模块，
> 消除 Feign 客户端重复定义与 mrb-common 归属混乱，建立「api 契约 jar + 服务实现」的双模块边界。

## 需求摘要

1. **消除 Feign 客户端重复**：`FileFeignClient` 在 mrb-meeting / mrb-user 各有一份逐字重复；`UserFeignClient` 在 mrb-auth / mrb-meeting 各有一份（方法集重叠）。
2. **消除 mrb-common 归属混乱**：`NotificationSendDTO`/`NotificationMessage`/`MqConstant` 实属平台域、`ReservationStatusEnum`/`AttendeeStatusEnum` 实属会议室域、`AuthUserDTO` 实属用户域，却堆在 mrb-common。
3. **彻底消除跨服务通知发送重复**：`NotificationSender`+`NotificationProducer` 原只在 mrb-meeting，其他服务需发送通知时无处复用，易重复实现降级逻辑。
4. **建立 api 契约边界**：跨服务契约抽离为独立 jar，服务实现模块仅依赖所需 api，依赖方向清晰。

## 技术变更清单

### 一、新增 3 个 api 契约模块

| 模块 | 包根 | 内容 |
|------|------|------|
| `mrb-user-api` | `com.meetinghub.user.api` | `UserFeignClient`（合并 auth/meeting 两处）、`DepartmentFeignClient`、`AuthUserDTO`、`UserBriefDTO`、`DepartmentBriefDTO` |
| `mrb-platform-api` | `com.meetinghub.platform.api` | `NotificationFeignClient`、`FileFeignClient`（合并 meeting/user 两处）、`NotificationSendDTO`、`NotificationMessage`、`MqConstant`、`NotificationSender`、`NotificationProducer` |
| `mrb-meeting-api` | `com.meetinghub.meeting.api` | `ReservationStatusEnum`、`AttendeeStatusEnum` |

各 api 模块 pom 依赖：`mrb-common` + 契约所需（user/platform-api 含 openfeign+loadbalancer；platform-api 含 rocketmq；meeting-api 仅 lombok）。

### 二、根 pom 注册新模块

`backend/pom.xml` <modules> 调整为：`mrb-common`、`mrb-gateway`、3 个 api 模块（先于服务构建）、4 个服务模块。api 先于服务构建，保证服务可依赖。

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

### 四、服务模块增补 api 依赖

| 服务 | 新增依赖 |
|------|---------|
| `mrb-auth` | `mrb-user-api`（UserFeignClient + AuthUserDTO） |
| `mrb-meeting` | `mrb-meeting-api` + `mrb-user-api` + `mrb-platform-api` |
| `mrb-platform` | `mrb-platform-api`（NotificationConsumer 用 MqConstant/NotificationMessage） |
| `mrb-user` | `mrb-platform-api`（FileFeignClient）+ `mrb-user-api`（UserController 实现 getUserForAuth 返回 AuthUserDTO） |

### 五、引用方 import 批量更新

对 17 个服务文件（meeting 的 service/tools/schedule/vo、platform 的 controller/service/mq、user 的 controller/service、auth 的 service + 测试）按 16 处包路径映射更新 import，均为确定性字符串替换，git 识别为 rename（保留历史）。

## 关键架构决策与对原计划的偏离

1. **采用平铺结构而非聚合器嵌套**：服务模块**不重命名**（保留 `mrb-auth`/`mrb-meeting`/`mrb-platform`/`mrb-user` 原名作为服务实现模块），新增 3 个 api 兄弟模块。理由：原计划「每个服务转为聚合器 + mrb-xxx-service 子模块」需 `git mv` 整个 src 树 + 重写服务 pom，风险高且收益低；平铺方案服务内部代码零迁移、零改动，仅新增 api 模块，更符合「不过度设计 / 避免破坏现有功能」。如需统一命名为 `mrb-xxx-service`，可作后续独立低风险重命名提交。
2. **不创建 mrb-auth-api**：auth 无任何跨服务 Feign 契约可暴露（无服务通过 Feign 调用 mrb-auth；网关用自有 JWT 过滤器，不走 Feign）。建空模块属过度设计，故跳过。原计划列 4 服务拆分，实际仅 3 个有跨服务契约。
3. **RedisKeyConstant 保留在 mrb-common**（偏离原计划「拆分」）：其字段跨域使用（`USER_TOKEN`→auth、`SCHEDULE_*`→meeting、`MQ_DEDUP`/`PREFIX`→platform），集中管理更利于红线 #2（`mrb:` 前缀统一管控），拆分反而分散前缀管控、增加维护成本。`DateTimePatternConstant` 同理留 common。

## 组件扫描与运行时说明

- 所有 Application 类均 `scanBasePackages = "com.meetinghub"` + `@EnableFeignClients`（默认扫描 com.meetinghub），api 模块的 Feign 客户端与 `@Component`（NotificationSender/Producer）会被消费方自动扫描装配，无需改启动类配置。
- `NotificationSender`/`NotificationProducer` 作为 `@Component` 落在 platform-api：依赖 platform-api 的服务（meeting/user/platform）均已有 rocketmq + feign 依赖，可正常装配。platform-service 与 user-service 会装配出未使用的 NotificationSender bean，无副作用（不调用即不触发）。

## 冲突与风险

- **行为零变更**：纯结构重构，无业务逻辑改动。Feign 客户端合并后方法集为两者并集（getUserForAuth 在 auth/meeting 两处签名一致），调用方行为不变。
- **UserFeignClient bean 合并**：原 auth-service 与 meeting-service 各持一个 `@FeignClient(name="mrb-user")`（不同 JVM，无冲突）；合并为 user-api 单一客户端后，各服务通过 classpath 引用，仍各自单实例，无 bean 冲突。DepartmentFeignClient 保留 `contextId="departmentFeignClient"` 与 UserFeignClient 共存（沿用原模式）。
- **Feign 自调用**：platform-service 装配的 NotificationSender 降级路径会 Feign 调用自身（mrb-platform），属回环但不致命（仅在 MQ 失败时触发，经负载均衡回自身端点）。
- **mrb-common 瘦身**：移除 6 个归属混乱类后，`common/model/dto/` 目录清空（其余 common 内容：Result、BusinessException、ErrorCode、UserContext、拦截器、RequiresRole 注解、配置类、CommonConstant、DateTimePatternConstant、RedisKeyConstant、其余 enums 均保留）。

## 验证点

1. **全量编译**：`cd backend && mvn -q compile -DskipTests` 通过
2. **测试编译**：`mvn -q test-compile -DskipTests` 通过（auth 测试引用迁移后的 UserFeignClient/AuthUserDTO）
3. **无残留旧引用**：grep 确认服务模块中无 `com.meetinghub.common.{model.dto.AuthUserDTO,model.dto.NotificationSendDTO,model.dto.NotificationMessage,constant.MqConstant,enums.ReservationStatusEnum,enums.AttendeeStatusEnum}` 及 `meeting.feign./meeting.mq.producer./auth.feign./user.feign.` 残留
4. **提交分阶段**：Stage1 骨架（4 files）、Stage2 内容迁移（37 files）
