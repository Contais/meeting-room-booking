# 050-gateway-jwt-verification 技术方案

## 背景

网关 `AuthGlobalFilter` 是唯一的外部认证边界（生产环境仅暴露 8080 端口），但当前只做 JWT 载荷解码，
不验签，导致任意伪造 token 可注入 `X-User-*` 头，下游服务无条件信任，形成认证绕过。

## 方案选择

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| A. 网关注入共享密钥验签 | 无额外 RPC、低延迟、实现简单 | 密钥需与 auth 服务共享 | 采用 |
| B. 网关调 auth 服务校验 Redis token | 单一鉴权源 | 每次请求 RPC、放大延迟与故障点 | 不采用（作为 A 的补充校验而非主路径） |
| C. 下游服务自行验签 | 纵深防御 | 改动面大、重复逻辑多 | 暂不采用，由网关统一处理 |

最终方案：**A + Redis 一致性校验**。

- 网关使用与 auth 服务相同的 HMAC 密钥验签（jjwt `verifyWith`），自动校验签名与 `exp` 过期时间。
- 验签通过后，比对 Redis `mrb:user:token:{userId}` 与当前 token：
  - 一致 → 放行并注入 `X-User-*`；
  - 不一致/不存在 → 401（登出即时失效、token 被刷新后旧 token 失效）。
- 校验失败或 Redis 异常 → fail-closed（401），保证安全性。

## 数据流

```text
请求 -> AuthGlobalFilter
  ├─ 白名单路径？ -> 直接放行
  ├─ 提取 Bearer token（Header / WebSocket query param）
  ├─ GatewayJwtVerifier 验签 + 过期校验
  │    └─ 失败 -> 401 {code,message,data}
  ├─ TokenValidationService 比对 Redis mrb:user:token:{userId}
  │    └─ 不一致 -> 401
  ├─ 覆盖客户端 X-User-* 头，注入验签后的 claims
  └─ 转发下游
```

## 接口与类设计

### `com.meetinghub.gateway.config.JwtProperties`

```java
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
}
```

### `com.meetinghub.gateway.security.GatewayJwtVerifier`

```java
public record TokenClaims(Long userId, String username, String role) {}

@Component
public class GatewayJwtVerifier {
    public Optional<TokenClaims> verify(String token);
}
```

职责：jjwt 验签（`Keys.hmacShaKeyFor(secret)` + `parseSignedClaims`），解析 `sub/username/role`。
验签失败或过期一律返回 `Optional.empty()`，不抛异常。

### `com.meetinghub.gateway.security.TokenValidationService`

```java
@Component
public class TokenValidationService {
    private final GatewayJwtVerifier verifier;
    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Optional<TokenClaims>> validate(String token);
}
```

职责：验签 + Redis 一致性比对。Redis key 与 mrb-common `RedisKeyConstant.USER_TOKEN` 保持一致
（`mrb:user:token:{userId}`，网关不引入 mrb-common 以避免 Servlet 依赖污染 WebFlux，常量本地化并注释出处）。

### `AuthGlobalFilter`（修改）

- 保留白名单与 query param token 降级逻辑。
- 校验通过后，先 `remove` 客户端提交的 `X-User-Id/Role/Username`，再注入验签后的值。
- 校验失败：401 + `application/json` 响应体 `{"code":401,"message":"未授权或Token无效","data":null}`（符合红线 8）。
- 删除原 `extractPayload` / `extractJsonField`。

## 配置变更

`mrb-gateway/src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
jwt:
  secret: mrb-meeting-room-booking-jwt-secret-key-2024  # 与 auth 服务保持一致
```

生产部署时 `mrb-gateway-prod.yml` 需配置相同 `jwt.secret` 与 Redis 地址（已更新 `docs/DEPLOYMENT.md`）。

## 测试计划

- `GatewayJwtVerifierTest`：合法/篡改/过期/错密钥/畸形 token。
- `TokenValidationServiceTest`：验签失败、Redis 一致、Redis 缺失、Redis 异常。
- `AuthGlobalFilterTest`：白名单放行、缺 token、伪造 token、合法 token 注入头、客户端头被覆盖、WebSocket query token。
