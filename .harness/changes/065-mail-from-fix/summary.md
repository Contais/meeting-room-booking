# 065-mail-from-fix

## 需求摘要

修复密码找回验证码邮件发送失败问题：QQ 邮箱 SMTP 返回
`501 Mail from address must be same as authorization user`，
验证码邮件无法发出，接口以"未知异常"500 返回。

## 根因

`PasswordResetMailSender` 构造 `SimpleMailMessage` 时未设置发件人（From），
JavaMail 回退使用本地地址（如 `user@hostname`）作为信封发件人，
与 SMTP 授权账号 `mrb_mreasonchan@qq.com` 不一致，被 QQ 邮箱拒绝。

## 技术变更清单

- `backend/mrb-user/mrb-user-service/.../service/PasswordResetMailSender.java`
  - 新增 `from` 字段：`@Value("${spring.mail.username:}")`，默认取 SMTP 授权账号；
    未配置邮件时为空、跳过设置，保持"无 SMTP 降级日志输出"行为不变。
  - 发送前 `message.setFrom(from)`，满足 QQ 邮箱 From 与授权账号一致的要求。
  - 发送异常（`MailException`）转 `BusinessException`（错误码 500，
    提示"验证码邮件发送失败，请稍后重试"），不再以"未知异常"透出。

## 影响范围与风险

- 仅影响 mrb-user 密码找回发送验证码链路，无 DB / API 协议变更。
- 配置无需改动：`application-local.yml` 中 `spring.mail.username` 即授权账号。

## 验证

- `mvn -pl mrb-user/mrb-user-service -am compile` 通过。
- `mvn -pl mrb-user/mrb-user-service test -Dtest=PasswordResetServiceImplTest` 通过。
- 手工验证：重启服务后调用发送验证码接口，收到验证码邮件即通过。
