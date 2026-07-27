# 用户注册与登录 - 技术方案设计

## 1. 设计目标

修复需求分析中识别的 P0/P1 问题，完善注册登录链路的安全性和健壮性。

## 2. 整体架构

```
┌──────────┐    POST /api/auth/login     ┌──────────────┐
│          │ ──────────────────────────→  │   Gateway    │
│  Frontend│    POST /api/user/register   │   (8080)     │
│  (5173)  │ ──────────────────────────→  │              │
│          │    POST /api/auth/refresh    │  Auth Filter │
│          │ ──────────────────────────→  │  (Token校验) │
└──────────┘                              └──────┬───────┘
                                                 │
                          ┌──────────────────────┼──────────────────────┐
                          │                      │                      │
                   ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐
                   │   mrb-user  │←─Feign─│  mrb-auth   │        │ mrb-meeting │
                   │   (8081)    │        │   (8082)    │        │   (8083)    │
                   │             │        │             │        │             │
                   │ /user/**    │        │ /auth/**    │        │ /meeting/** │
                   └─────────────┘        └─────────────┘        └─────────────┘
                          │                      │
                          │               ┌──────▼──────┐
                          │               │    Redis    │
                          └──────────────→│  Token存储  │
                                          └─────────────┘
```

## 3. 变更设计

### 3.1 P0: 新建 AuthUserDTO（替代 Map）

**文件**: `mrb-auth/src/main/java/com/meetinghub/auth/model/dto/AuthUserDTO.java`

```java
@Data
public class AuthUserDTO implements Serializable {
    private Long id;
    private String username;
    private String password;  // BCrypt 哈希，仅鉴权内部使用
    private Integer status;
}
```

**变更**: `UserFeignClient` 返回类型从 `Result<Map<String, Object>>` 改为 `Result<AuthUserDTO>`。

**同时在 mrb-user 中**: 新增 `AuthUserDTO` 或复用（推荐在 user 模块建 DTO，auth 模块通过 Feign 反序列化）。实际上 DTO 应定义在 **mrb-common** 中，两个模块共用。

**最终方案**: 在 `mrb-common` 新建 `com.meetinghub.common.model.dto.AuthUserDTO`，user 和 auth 模块共用。

### 3.2 P0: 修复 /user/{id} 密码泄露

**当前**: `UserController.getUser()` 返回 `Result<User>`，User 实体含 password 字段。

**变更**: 返回 `Result<UserDTO>`，UserDTO 不含 password。

```java
@GetMapping("/{id}")
public Result<UserDTO> getUser(@PathVariable Long id) {
    User user = userService.getUserById(id);
    return Result.ok(toDTO(user));
}

private UserDTO toDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setPhone(user.getPhone());
    dto.setStatus(user.getStatus());
    return dto;
}
```

### 3.3 P0: 网关白名单确认

**当前 `AuthGlobalFilter.WHITE_LIST`**:
```java
private static final List<String> WHITE_LIST = List.of(
    "/api/auth/login",
    "/api/auth/register",
    "/api/user/register"
);
```

**检查结果**: `/api/user/register` 已在白名单中。无需变更。但 `/api/auth/refresh` 也需要 Token 校验（非放行），当前逻辑正确。

### 3.4 P1: 增强注册校验

**文件**: `UserServiceImpl.java`

新增校验逻辑：
1. **手机号唯一性**: `SELECT count(*) FROM user WHERE phone = ? AND deleted = 0`
2. **用户名格式**: 2-32位，字母/数字/下划线（正则 `^[a-zA-Z0-9_]{2,32}$`）
3. **手机号格式**: 11位手机号（正则 `^1[3-9]\\d{9}$`）

新增 ErrorCode:
- `USERNAME_FORMAT_ERROR(1009, "用户名格式不正确")`
- `PHONE_FORMAT_ERROR(1010, "手机号格式不正确")`
- `PHONE_ALREADY_EXISTS(1011, "手机号已注册")`

### 3.5 P1: 修复刷新接口参数传递

**当前**: AuthController.refreshToken 使用 `@RequestParam String token`

**变更**: 改为 `@RequestBody` 接收，与前端 axios 调用方式一致。

```java
@PostMapping("/refresh")
public Result<String> refreshToken(@RequestBody Map<String, String> params) {
    return Result.ok(authService.refreshToken(params.get("token")));
}
```

同步更新前端 `api/auth.ts`:
```typescript
export function refreshToken(token: string): Promise<Result<string>> {
  return request.post('/api/auth/refresh', { token })
}
```

### 3.6 P2: 登出接口（Token 失效）

**接口**: `POST /api/auth/logout`

**流程**:
1. 解析 Token 获取 userId
2. 删除 Redis 中 `mrb:user:token:{userId}`
3. 返回成功

**前端**: 在 MainLayout 的退出登录按钮中调用 logout API。

## 4. 文件变更清单

| 模块 | 文件 | 操作 | 优先级 |
|------|------|------|--------|
| mrb-common | `model/dto/AuthUserDTO.java` | 新建 | P0 |
| mrb-common | `exception/ErrorCode.java` | 新增 3 个错误码 | P1 |
| mrb-user | `controller/UserController.java` | 修复 getUser 返回 DTO | P0 |
| mrb-user | `controller/UserController.java` | 优化 getUserForAuth 返回 DTO | P0 |
| mrb-user | `service/impl/UserServiceImpl.java` | 增强注册校验 | P1 |
| mrb-auth | `model/dto/AuthUserDTO.java` | 删除（移到 common） | P0 |
| mrb-auth | `feign/UserFeignClient.java` | 返回类型改为 AuthUserDTO | P0 |
| mrb-auth | `service/impl/AuthServiceImpl.java` | 适配 AuthUserDTO | P0 |
| mrb-auth | `controller/AuthController.java` | 修复 refresh 参数 | P1 |
| mrb-auth | `service/AuthService.java` | 新增 logout 方法 | P2 |
| mrb-auth | `service/impl/AuthServiceImpl.java` | 实现 logout | P2 |
| mrb-auth | `controller/AuthController.java` | 新增 logout 端点 | P2 |
| mrb-gateway | `filter/AuthGlobalFilter.java` | 确认白名单（已OK） | P0 |
| frontend | `api/auth.ts` | 修复 refreshToken 参数 | P1 |
| frontend | `api/auth.ts` | 新增 logout API | P2 |
| frontend | `stores/user.ts` | logout 调用后端 API | P2 |
| frontend | `layouts/MainLayout.vue` | 退出登录调用 API | P2 |

## 5. 编码顺序

```
Phase 1 (P0 - 安全修复):
  common/AuthUserDTO → user/UserController修复 → auth/FeignClient适配 → 编译验证

Phase 2 (P1 - 功能增强):
  common/ErrorCode新增 → user/注册校验增强 → auth/refresh修复 → frontend/refresh修复

Phase 3 (P2 - 登出功能):
  auth/logout → frontend/logout
```

## 6. 验证计划

| 验证项 | 方法 | 期望 |
|--------|------|------|
| /user/{id} 不返回密码 | curl + 检查响应 JSON | 无 password 字段 |
| 重复用户名注册 | POST /api/user/register 同名用户 | 返回 1006 错误 |
| 重复手机号注册 | POST /api/user/register 同手机号 | 返回 1011 错误 |
| 登录成功 | POST /api/auth/login 正确凭证 | 返回 JWT Token |
| 登录失败-密码错误 | POST /api/user/register 错误密码 | 返回 "用户名或密码错误" |
| Token 刷新 | POST /api/auth/refresh 有效 Token | 返回新 Token |
| 网关拦截 | GET /api/user/1 无 Token | 返回 401 |
| 全模块编译 | mvn clean compile | BUILD SUCCESS |
