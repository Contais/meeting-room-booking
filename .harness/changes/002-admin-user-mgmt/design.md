# 管理端用户管理 - 技术方案设计

## 1. 角色模型设计

```java
// mrb-common 新增
public enum RoleEnum {
    ADMIN("admin", "管理员"),
    USER("user", "普通用户");

    private final String code;
    private final String desc;
}
```

User 实体新增 `role` 字段，VARCHAR(20)，默认 "user"。

## 2. JWT 携带角色

**变更 AuthServiceImpl.login()：**
```
签发 Token 时: Jwts.builder().claim("role", user.getRole())...
```

**变更 JwtUtils：**
```
新增 getRoleFromToken(String token): String
```

## 3. 角色校验方案

**新增注解 + 拦截器方式（可扩展）：**

```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String value();
}

// HandlerInterceptor 拦截器
// 从请求头获取 Token → 解析 role → 校验是否匹配
```

注册到 Spring MVC: `WebMvcConfigurer.addInterceptors()`

**备选方案（更简单）：Controller 内部判断**
```java
@GetMapping("/admin/list")
public Result<?> listUsers(...) {
    // 从请求头解析 role，非 admin 直接抛异常
}
```
→ 不够优雅，但现阶段够用。推荐用注解方式。

## 4. 管理接口设计

### 4.1 分页查询

```
GET /api/user/admin/list?page=1&size=10&keyword=&status=
```

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认10 |
| keyword | String | 否 | 用户名/手机号模糊搜索 |
| status | Integer | 否 | 状态筛选：0-禁用 1-启用 |

**响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 4.2 新增用户

```
POST /api/user/admin/create
```
```json
{
  "username": "zhangsan",
  "password": "123456",
  "phone": "13800138000",
  "role": "user",
  "realName": "张三"
}
```

### 4.3 编辑用户

```
PUT /api/user/admin/update
```
```json
{
  "id": 1,
  "phone": "13900139000",
  "role": "admin",
  "realName": "张三"
}
```
→ 不允许修改用户名和密码

### 4.4 启用/禁用

```
PUT /api/user/admin/toggle-status/{id}
```
→ 切换 status 0↔1

### 4.5 逻辑删除

```
DELETE /api/user/admin/delete/{id}
```
→ 检查未完成预约 → 标记 deleted=1 → 清除 Redis Token

## 5. 前端路由守卫

```typescript
// router/index.ts
{
  path: '/admin',
  meta: { requiresAuth: true, requiresAdmin: true },
  children: [
    { path: 'users', component: () => import('@/views/admin/UserManage.vue') }
  ]
}

// beforeEach 守卫中检查
if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'admin') {
  ElMessage.error('无权访问')
  next('/home')
}
```

## 6. 文件变更清单

| 模块 | 文件 | 操作 |
|------|------|------|
| mrb-common | model/entity/User.java 或枚举 RoleEnum | 新增 |
| mrb-user | model/entity/User.java | 加 role, realName 字段 |
| mrb-user | model/dto/ 新增 UserCreateDTO, UserUpdateDTO, UserPageQuery | 新增 |
| mrb-user | controller/UserController.java | 新增管理接口 |
| mrb-user | service/UserService.java | 新增管理方法 |
| mrb-user | service/impl/UserServiceImpl.java | 实现 |
| mrb-auth | service/impl/AuthServiceImpl.java | JWT 携带 role |
| mrb-auth | util/JwtUtils.java | 新增 getRoleFromToken |
| mrb-auth | model/dto/LoginDTO 或响应 | 登录返回 role |
| backend | sql/init.sql | admin 账号 role='admin' |
| frontend | views/admin/UserManage.vue | 用户管理页面 |
| frontend | router/index.ts | admin 路由守卫 |
| frontend | api/user.ts | 新增管理接口 |
| frontend | types/user.d.ts | 加 role 字段 |
| frontend | layouts/MainLayout.vue | 角色菜单 |

## 7. 编码顺序

```
Phase 1 (P0 后端基础):
  User实体加role → JWT携带role → 管理接口CRUD → 角色校验 → 编译

Phase 2 (P1 前端):
  类型更新 → API封装 → 用户管理页面 → 路由守卫 → 菜单

Phase 3 (P2 完善):
  逻辑删除预约检查 → 联调测试
```
