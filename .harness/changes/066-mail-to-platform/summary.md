# 066-mail-to-platform

## 需求摘要

邮件发送能力从 mrb-user 下沉至 mrb-platform（消息通知域）：
当前仅密码重置验证码一个场景，但邮件属通用基础能力，
后续预约提醒、审批通知等场景可直接复用平台邮件 API，
避免各业务服务重复配置 SMTP、耦合邮件依赖。

## 技术变更清单

- `mrb-common` `ErrorCode`：新增 `MAIL_NOT_CONFIGURED(1027, "邮件服务未配置")`
- `mrb-platform-api`：
  - 新增 `model/dto/MailSendDTO`（to/subject/content 通用三字段）
  - 新增 `feign/MailFeignClient`（`POST /platform/internal/mail/send`，`Result<Void>`）
- `mrb-platform-service`：
  - 新增 `service/MailService` + `impl/MailServiceImpl`（ObjectProvider 弱依赖 JavaMailSender；
    From=spring.mail.username；未配置抛 MAIL_NOT_CONFIGURED；发送失败抛业务异常）
  - 新增 `controller/MailInternalController`（`/platform/internal/mail/**`，仅服务间 Feign）
  - pom 新增 `spring-boot-starter-mail`
  - `application-local.yml` 迁入 QQ SMTP 配置（自 mrb-user）
- `mrb-user-service`：
  - `PasswordResetMailSender` 改为经 `MailFeignClient` 调用，容错：
    未配置(1027)/Feign 异常 → 日志兜底输出验证码；真实发送失败 → BusinessException
  - pom 移除 `spring-boot-starter-mail`
  - `application-local.yml` 移除 spring.mail 段

详细设计见 [design.md](design.md)。

## 冲突与风险

- mrb-user 依赖 mrb-platform 可用性：platform 不可用时验证码降级日志输出，主流程不受阻。
- 部署顺序要求：先启动/升级 mrb-platform，再重启 mrb-user。
- 无 DB、无前端、无网关路由变更（internal 路径不经网关）。

## 验证

- 后端全量 `mvn -DskipTests compile` 通过
- `PasswordResetServiceImplTest` 通过；mrb-platform-service 全量测试通过
- 手工验证：同时启动 platform(local) 与 user(local)，忘记密码发送验证码，
  收到 `mrb_mreasonchan@qq.com` 邮件且 user 日志输出 `密码重置验证码邮件已发送`；
  停掉 platform 再发送，user 日志出现兜底验证码输出且接口不报错。
