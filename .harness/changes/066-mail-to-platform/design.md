# 066-mail-to-platform 设计方案

## 背景与动机

064（忘记密码）将邮件发送实现在 mrb-user（`PasswordResetMailSender` 直连 SMTP）。
邮件属于通用基础能力，与项目约定「文件存储、消息通知、字典、系统配置归 mrb-platform」
一致应下沉至平台服务；后续任何业务（预约提醒、审批通知等）需要发邮件时可直接复用，
避免各业务服务重复配置 SMTP 并耦合 `spring-boot-starter-mail`。

## 架构决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 承载服务 | mrb-platform（消息通知域） | 与通知/文件等基础能力同域，SMTP 凭证集中管理 |
| 调用方式 | 同步 Feign（不走 MQ） | 验证码等场景要求即时反馈结果；站内信 NotificationSender 的 MQ 优先策略面向批量异步场景，不适用 |
| API 形态 | 通用三字段 `MailSendDTO{to, subject, content}` | 不与"验证码"语义耦合，后续邮件场景直接复用 |
| 路径 | `POST /platform/internal/mail/send` | 符合工程结构规范：内部 Feign 接口统一 `<服务前缀>/internal/<resource>/**`，不经过网关 |
| 未配置 SMTP 的语义 | platform 抛 `MAIL_NOT_CONFIGURED(1027)` → 调用方降级 | 验证码兜底日志必须在调用方（验证码语义在 user 域）；本地开发无需起邮件服务器 |
| 发件人 | `spring.mail.username`（SMTP 授权账号） | QQ 邮箱等要求 From 与授权账号一致，否则 501（见 065） |

## 接口设计

```
POST /platform/internal/mail/send
Content-Type: application/json

{ "to": "user@example.com", "subject": "...", "content": "..." }

→ Result<Void>
  code=200            发送成功
  code=1027 MAIL_NOT_CONFIGURED  平台未配置 SMTP
  code=500            SMTP 发送失败 / 参数为空（400）
```

## 调用方容错策略（mrb-user PasswordResetMailSender）

1. `code=200` → 成功；
2. `code=1027`（未配置）→ warn 日志输出验证码兜底，流程继续（本地开发可走通）；
3. 其他非 200 → `BusinessException("验证码邮件发送失败，请稍后重试")`，用户可感知并重试；
4. Feign 调用异常（platform 不可用）→ warn 日志输出验证码兜底，流程继续。

## 涉及模块

- `mrb-common`：ErrorCode 新增 `MAIL_NOT_CONFIGURED(1027)`
- `mrb-platform-api`：`MailSendDTO`、`MailFeignClient`
- `mrb-platform-service`：`MailService`/`MailServiceImpl`、`MailInternalController`、pom 增加 mail starter、local 配置迁入 SMTP
- `mrb-user-service`：`PasswordResetMailSender` 改走 Feign、pom 移除 mail starter、local 配置移除 SMTP

## 风险与兼容

- mrb-user 不再本地发信：需 mrb-platform 运行中；不可用时验证码降级日志，不影响主流程。
- 部署顺序：先升级 mrb-platform（新增接口向后兼容），再升级 mrb-user。
