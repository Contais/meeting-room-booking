# 全仓统一 RedisKeyConstant 限定名引用

> 将全仓对 `RedisKeyConstant` 成员的引用统一为限定名 `RedisKeyConstant.X` 直接使用：
> 删除所有 `import static`，与 046（DateTimePatternConstant 限定名统一）保持同一代码风格约定。

## 需求摘要

1. **统一引用风格**：对 `RedisKeyConstant` 各 Key 常量的引用一律使用限定名，禁止 `import static`。
2. **不改变行为**：Key 前缀 `mrb:` 与各常量值不变，纯代码风格重构。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `AuthServiceImpl.java`（mrb-auth-service） | 删除 `import static ...RedisKeyConstant.USER_TOKEN`，新增普通导入，3 处使用点（登录/刷新/登出）改限定名 `RedisKeyConstant.USER_TOKEN` |

其余引用点（`ReservationScheduleTask`、`NotificationConsumer`、`SysConfigServiceImpl`）本已使用限定名，无需修改；
`SysConfigServiceImpl.CACHE_PREFIX` 为「PREFIX + 后缀」的派生常量（非纯别名），保留。

## 冲突与风险

- 纯机械重构，无业务逻辑变更。
- 该文件位于 mrb-auth-service 模块，改动后需重新编译该模块确认无遗漏。

## 验收标准

- `rg "import static .*RedisKeyConstant" backend` 无结果。
- 除常量类自身外，无裸 `USER_TOKEN` / `MQ_DEDUP` / `SCHEDULE_LOCK` 等常量引用。
- `mvn -pl mrb-auth/mrb-auth-service -am compile`（或 test）通过。
