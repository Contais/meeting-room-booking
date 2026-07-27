# 用户注册与登录 - 需求分析报告

## 1. 需求描述

实现会议室预约系统的用户认证体系，支持用户注册账号和登录获取 JWT Token，为后续会议室预约等功能提供身份认证基础。

### 验收标准

| 编号 | 验收条件 |
|------|----------|
| AC-1 | 用户可通过 POST /api/user/register 注册新账号，密码 BCrypt 加密存储 |
| AC-2 | 注册时校验用户名唯一性，重复注册返回明确错误 |
| AC-3 | 用户可通过 POST /api/auth/login 登录，返回有效 JWT Token |
| AC-4 | 登录校验用户名存在性、密码正确性、账号启用状态 |
| AC-5 | Token 存储在 Redis，支持踢人下线（同用户重复登录覆盖旧 Token） |
| AC-6 | 已登录用户可通过 /api/auth/refresh 刷新 Token |
| AC-7 | 网关正确放行 /api/auth/login 和 /api/user/register，其余路径需 Token |
| AC-8 | 前端登录页能走通完整登录流程，登录后跳转首页 |

## 2. 技术变更清单

### 2.1 后端（已有实现，需完善）

| 变更项 | 文件 | 变更类型 | 说明 |
|--------|------|----------|------|
| UserDTO 返回体 | `UserDTO.java` | 已有 | 不含密码，安全 |
| getUserForAuth 内部接口 | `UserController.java` | 需优化 | 当前用 `Map<String, Object>` 返回，应改为强类型 DTO |
| getUser/{id} 接口 | `UserController.java` | 需修复 | 当前返回完整 User 实体（含密码哈希），应返回 UserDTO |
| 注册校验 | `UserServiceImpl.java` | 需增强 | 缺少手机号唯一性校验、用户名格式校验 |
| 登录接口 | `AuthServiceImpl.java` | 已实现 | Feign→BCrypt→JWT→Redis 完整流程 |
| 刷新接口 | `AuthController.java` | 需修复 | 当前用 `@RequestParam`，应改为 `@RequestBody` 与前端一致 |
| 网关白名单 | `AuthGlobalFilter.java` | 需确认 | 需包含 `/api/user/register` |
| ErrorCode | `ErrorCode.java` | 已有 | USER_NOT_FOUND(1005)、USER_ALREADY_EXISTS(1006)、AUTH_TOKEN_INVALID(1007) |
| SQL 初始化 | `init.sql` | 已有 | 测试用户 admin/123456, test/123456 |

### 2.2 前端（已有实现）

| 变更项 | 文件 | 状态 |
|--------|------|------|
| 登录页 | `LoginView.vue` | 已完成 |
| 登录 API | `api/auth.ts` | 已完成（JSON Body） |
| Axios 拦截器 | `utils/request.ts` | 已完成（Token 注入） |
| 路由守卫 | `router/index.ts` | 已完成 |
| 用户 Store | `stores/user.ts` | 已完成 |

## 3. 业务影响范围

### 受影响的模块

| 模块 | 影响 |
|------|------|
| mrb-user | 用户注册、用户信息查询 |
| mrb-auth | 登录认证、Token 签发与刷新 |
| mrb-gateway | 请求路由、Token 校验过滤器 |
| frontend | 登录页、路由守卫、Token 管理 |

### 不受影响的模块

- mrb-meeting（会议室管理、预约逻辑暂不涉及）

## 4. 冲突与风险

### 4.1 安全风险

| 风险 | 严重度 | 说明 | 缓解措施 |
|------|--------|------|----------|
| 密码哈希泄露 | 高 | `/user/{id}` 当前返回完整 User 实体，含 password 字段 | 改为返回 UserDTO |
| 内部接口暴露 | 中 | `/user/internal/info/username/{username}` 无鉴权保护 | 仅限内网调用，网关不暴露此路径 |
| JWT Secret 硬编码 | 中 | application.yml 中直接写入密钥 | 生产环境通过 Nacos 配置中心注入 |

### 4.2 架构风险

| 风险 | 严重度 | 说明 | 缓解措施 |
|------|--------|------|----------|
| Feign 调用超时 | 低 | Auth→User Feign 调用失败导致登录不可用 | 配置合理超时 + 降级 |
| Redis 不可用 | 高 | Token 存储依赖 Redis，宕机导致登录失败 | Redis 部署 Sentinel/Cluster |

### 4.3 已知技术债

- `getUserForAuth` 使用 `Map<String, Object>` 返回，类型不安全，应改为 `AuthUserDTO`
- 注册接口缺少手机号唯一性校验
- 无登出接口（Token 黑名单机制未实现）
- 无注册频率限制（防刷）

## 5. 任务拆分建议

| 优先级 | 任务 | 预估 |
|--------|------|------|
| P0 | 修复 `/user/{id}` 返回 UserDTO（不泄露密码） | 小 |
| P0 | 优化 `getUserForAuth` 改为强类型 DTO | 小 |
| P0 | 确认网关白名单包含注册路径 | 小 |
| P1 | 增强注册校验（手机号唯一、用户名格式） | 中 |
| P1 | 修复刷新接口参数传递方式 | 小 |
| P2 | 实现登出接口（Token 失效） | 中 |
| P2 | 注册频率限制 | 小 |
