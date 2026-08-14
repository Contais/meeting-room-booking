# 064-forgot-password 需求分析摘要

## 需求描述

修复登录页「忘记密码」死链，提供邮箱验证码自助找回密码流程，并处理用户未绑定邮箱的场景。

方案要点（已与用户确认）：
- 用户输入用户名后发送 6 位验证码到账号绑定邮箱；未绑定邮箱时返回明确提示，引导联系管理员。
- 验证码存 Redis（key 前缀 `mrb:`），5 分钟有效，发送有 60 秒冷却，校验失败最多 5 次。
- 校验通过后允许设置新密码（6-64 位），成功后删除验证码相关 Redis key。
- 未配置 SMTP 时降级为日志输出验证码，便于本地开发/演示；配置 SMTP 后真实发送邮件。

## 验收标准

- [ ] 登录页「忘记密码」不再是死链，点击弹出找回密码表单
- [ ] 输入用户名并发送验证码：已绑定邮箱返回成功提示，未绑定邮箱返回 1024「该账号未绑定邮箱」
- [ ] 校验成功后可设置新密码，随后可用新密码登录
- [ ] 验证码错误返回 1025，连续错误超过 5 次返回 1026
- [ ] 发送接口有 60 秒冷却，避免重复轰炸
- [ ] 网关白名单放行 `/api/uc/user/forgot-password/**`，未登录可访问
- [ ] 新增 `PasswordResetServiceImplTest` 通过；前端 `npm run build` 通过

## 技术变更清单

| 变更 | 说明 |
|------|------|
| `ErrorCode`（mrb-common） | 新增 1024/1025/1026 密码找回错误码 |
| `RedisKeyConstant`（mrb-common） | 新增 forgot-pwd code/cooldown/attempts 三个 key（mrb: 前缀） |
| `mrb-user-service/pom.xml` | 引入 spring-boot-starter-mail |
| `ForgotPasswordSendDTO` / `ForgotPasswordResetDTO`（新增） | 找回密码请求 DTO |
| `PasswordResetService` / `PasswordResetServiceImpl`（新增） | 发送验证码、校验并重置密码 |
| `PasswordResetMailSender`（新增） | SMTP 发送，未配置时降级日志输出 |
| `UserController` | 新增两个匿名找回密码接口 |
| `AuthGlobalFilter`（网关） | 白名单放行 `/api/uc/user/forgot-password` |
| `mrb-user-service/application.yml` | 增加 SMTP 配置示例（默认注释） |
| `frontend/src/api/user.ts` | 新增发送验证码与按验证码重置密码接口 |
| `frontend/src/views/auth/LoginView.vue` | 找回密码弹窗 + 发送倒计时 + 前端校验 |
| `.harness/wiki/接口协议.md` | 新增接口、白名单、错误码说明 |

## 业务影响范围

- 仅用户登录入口与用户服务；无 DB 结构变更、无 MQ 变更。
- 新增邮件依赖，默认不影响现有环境启动（无 SMTP 时验证码走日志）。

## 冲突与风险

- **用户枚举**：发送接口对「用户不存在」与「未绑定邮箱」返回不同提示，会暴露账号是否存在；内部系统可接受，但若需收紧可改为统一模糊提示。
- **SMTP 未配置**：生产环境必须配置 SMTP 才会真实发信；未配置时验证码仅打印在后端日志，属开发/演示用途。
- **验证码安全**：验证码 6 位数字 + 5 分钟有效 + 60 秒冷却 + 5 次错误上限，已做基础防护；更高安全要求可改为更长效的 token 链接。
