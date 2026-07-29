# 038 - COS 预签名 URL 改造方案（二期）

> 一期已采用「前缀公开读」临时解决访问问题。二期目标：桶保持私有，按需生成带签名临时访问链接，提升安全性。

## 一、背景与目标

| 项 | 一期（前缀公开读） | 二期（预签名 URL） |
|----|--------------------|--------------------|
| 桶权限 | avatar/、room/ 前缀公开读 | 桶完全私有 |
| URL 有效期 | 永久 | 可配置（如 1 小时） |
| 安全性 | 低（链接泄露即永久暴露） | 高（过期失效） |
| DB 存储 | 完整 URL | objectKey（相对路径） |
| 前端改动 | 无 | 无（仍用 URL，但带签名 query） |

## 二、核心矛盾

当前 `FileStorageService.store()` 返回完整访问 URL 并直接落库（avatar、room.image 字段）。预签名 URL 有有效期，不能存死链接，必须：
- **DB 只存 objectKey**（如 `avatar/202607/uuid.png`）
- **读取时动态生成签名 URL** 返回给前端

## 三、改动清单（改动量评估）

### 3.1 抽象层 `mrb-common`（改动小）

`FileStorageService` 接口新增方法：

```java
/**
 * 生成预签名访问 URL（私有桶按需授权）
 * @param objectKey 对象键
 * @param expireSeconds 有效期秒数
 */
String generatePresignedUrl(String objectKey, long expireSeconds);
```

- `CosFileStorageService`：用 `cosClient.generatePresignedUrl(bucket, key, new Date(expire))` 生成，有效期默认 3600s。
- `LocalFileStorageService`：本地无鉴权，直接返回 `urlPrefix + "/" + objectKey`。
- `store()` 返回值语义从「访问 URL」改为「objectKey」（破坏性变更，需配合 3.3 数据迁移）。

### 3.2 配置层（改动小）

`FileStorageProperties.Cos` 新增：
```yaml
file:
  storage:
    cos:
      presigned-expire: 3600   # 预签名有效期秒数
```

### 3.3 数据库迁移（改动中）

现有 `sys_user.avatar`、`meeting_room.image` 存的是完整 URL，需剥成 objectKey：

```sql
-- 剥离 COS 域名前缀，仅保留 objectKey
UPDATE sys_user SET avatar = REPLACE(avatar, 'https://mrb-1310160539.cos.ap-guangzhou.myqcloud.com/', '') WHERE avatar LIKE 'https://%';
UPDATE meeting_room SET image = REPLACE(image, 'https://mrb-1310160539.cos.ap-guangzhou.myqcloud.com/', '') WHERE image LIKE 'https://%';
```

兼容策略：读取时判断字段是否以 `http` 开头——是则按一期公开链接原样返回（过渡期），否则按 objectKey 生成签名 URL。

### 3.4 读取侧转换（改动中，重点）

凡向前端返回 avatar / image 的 VO 组装处，需 objectKey → 签名 URL：

| 位置 | 说明 |
|------|------|
| `UserService` 查用户信息 / 用户列表 | avatar 字段转换 |
| `UserFeignClient` 跨服务返回的 BriefDTO | avatar 是否需要签名（视暴露面定） |
| `RoomService` 查会议室列表 / 详情 | image 字段转换 |
| `FileController` 上传成功响应 | 返回 objectKey 给前端，前端不再直接用此 URL 渲染 |

建议封装工具方法 `FileUrlResolver.resolve(String storedValue)`：
- `null`/空 → 默认占位图
- 以 `http` 开头 → 原样返回（兼容旧数据 / 本地模式 URL）
- 否则 → `fileStorageService.generatePresignedUrl(value, expire)`

### 3.5 性能考量

- 预签名是本地 HMAC 计算（无网络 IO），单次 < 1ms，列表页 N 条可接受。
- 可选优化：对同一 objectKey 在有效期窗口内做内存缓存（Caffeine，TTL 略小于签名有效期），避免重复签名。

### 3.6 前端（改动无）

前端 `<img :src="url">` 不变，签名 URL 自带 query 参数，确保请求拦截器不要对图片 URL 做改写。

## 四、实施步骤建议

1. 抽象层加 `generatePresignedUrl` + 配置项（向后兼容，不破坏一期）
2. 上传链路 `store()` 返回 objectKey，DB 新数据存 objectKey
3. 读取侧加 `FileUrlResolver`，新旧数据兼容
4. 执行 SQL 迁移旧数据为 objectKey
5. 控制台撤掉前缀公开读策略，桶回归私有
6. 验证头像 / 会议室图片 / 上传预览全链路

## 五、风险

- **跨服务 BriefDTO**：若 mrb-meeting 通过 Feign 拿到的 UserBriefDTO 含 avatar，是否需签名取决于是否直接展示。如展示则需在 mrb-user 侧签名后再返回。
- **签名有效期与前端缓存**：若前端长期缓存图片 URL，过期后会 403。建议前端图片 URL 失效时静默刷新（或在列表接口每次返回最新签名）。
- **本地模式**：本地存储无签名概念，`generatePresignedUrl` 返回静态 URL，需保证本地与云模式行为一致。

## 六、改动量总结

| 模块 | 文件数 | 改动量 |
|------|--------|--------|
| mrb-common 抽象层 | 3（接口 + COS 实现 + Local 实现） | 小 |
| mrb-common 配置 | 1（Properties） | 小 |
| mrb-user 读取侧 | 2~3（UserService / UserVO 组装） | 中 |
| mrb-meeting 读取侧 | 2（RoomService / RoomVO 组装） | 中 |
| DB 迁移 SQL | 1 | 小 |
| 前端 | 0 | 无 |

预估总改动 8~10 个文件，集中在读取侧 VO 组装。无破坏性前端改动。
