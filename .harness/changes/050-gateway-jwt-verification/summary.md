# 050-gateway-jwt-verification 需求分析摘要

## 需求描述

修复网关认证绕过漏洞（严重）：

- 现状：`AuthGlobalFilter` 只对 JWT 做三段拆分 + base64 解码 payload，不验签、不过期校验、不查 Redis，
  直接把 `sub/role/username` 注入 `X-User-*` 头；下游 `UserContextInterceptor` / `RoleInterceptor` 完全信任这些头。
- 后果：攻击者用任意三段 base64 伪造 token 即可冒充任意用户（含 `ROLE_ADMIN`），绕过管理员权限校验。
- 目标：网关注入共享密钥验签（HMAC）+ 过期校验 + Redis token 一致性校验；同时清理客户端自带的 `X-User-*` 头。

## 验收标准

- [ ] 伪造签名/篡改 payload/过期 token 一律返回 401，不注入 `X-User-*` 头
- [ ] 签名有效但 Redis 中 token 不一致（已登出/被刷新）返回 401
- [ ] 合法 token 正常透传，`X-User-Id/Role/Username` 与 JWT claims 一致
- [ ] 客户端自带的 `X-User-*` 头被网关覆盖，无法伪造
- [ ] 白名单路径（登录/注册/静态资源）不受影响
- [ ] WebSocket query param token 同样经过验签与 Redis 校验
- [ ] 网关模块单元测试通过，CI 门禁通过

## 技术变更清单

| 变更 | 说明 |
|------|------|
| `mrb-gateway/pom.xml` | 新增 jjwt 0.12.5、spring-boot-starter-data-redis、spring-boot-starter-test |
| `mrb-gateway/application.yml` | 新增 `jwt.secret`（与 auth 服务一致）与 Redis 连接配置 |
| `GatewayJwtVerifier`（新增） | jjwt 验签 + 过期校验 + claims 解析 |
| `TokenValidationService`（新增） | 验签后比对 Redis `mrb:user:token:{userId}`，返回校验结果 |
| `AuthGlobalFilter`（修改） | 校验通过才注入头；覆盖客户端 `X-User-*`；失败返回 401 JSON |
| 网关测试基础设施（新增） | `mockito-extensions/org.mockito.plugins.MockMaker` 使用 subclass mock maker，兼容本机 Homebrew JDK 无法 attach 的环境 |
| `docs/DEPLOYMENT.md` | 网关生产配置补充 `jwt.secret` 与 Redis 配置说明 |

## 业务影响范围

- 全部需鉴权的 API（经网关进入）与 WebSocket 握手。
- 无 DB 变更、无前端变更、无 MQ 变更。

## 冲突与风险

- **生产部署**：`mrb-gateway-prod.yml` 必须配置与 `mrb-auth-prod.yml` 相同的 `jwt.secret`，且网关可访问 Redis，否则全部请求 401（fail-closed）。
- **密钥管理**：`jwt.secret` 目前硬编码在 yml，本变更保持与 auth 服务一致；建议后续收敛到环境变量/Nacos 配置中心。
- **Redis 可用性**：校验依赖 Redis，Redis 故障时网关拒绝放行（安全优先），与 auth 服务登录链路一致。
- **多端登录**：沿用现有"单用户单 token"语义（login/refresh 会覆盖 Redis 中的旧 token）。
