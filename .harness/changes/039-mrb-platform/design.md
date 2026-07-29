# 039 - mrb-platform 技术方案

## 一、架构决策

### 1.1 服务边界

```
mrb-gateway (8080)
   ├── /api/auth/**        → mrb-auth (8082)
   ├── /api/uc/**          → mrb-user (8081)
   ├── /api/uc/user/notification/** → mrb-platform (8084)  ← 通知分流
   ├── /api/meeting/**     → mrb-meeting (8083)
   ├── /api/file/**        → mrb-platform (8084)           ← 文件存储
   ├── /api/platform/**    → mrb-platform (8084)           ← 字典/配置
   └── /ws/**              → mrb-platform (8084)           ← WebSocket
```

### 1.2 域归属

| 域 | 原归属 | 现归属 |
|----|--------|--------|
| 文件存储 | mrb-common（共享库） | mrb-platform（独立服务） |
| 消息通知 + WebSocket | mrb-user | mrb-platform |
| 字典 | 无 | mrb-platform（新建） |
| 系统配置 | 无 | mrb-platform（新建） |

## 二、COS 预签名方案

### 2.1 写入侧

```
前端上传 → FileController.upload → FileServiceImpl.upload
  → fileStorageService.store(bytes, objectKey, contentType)  // 返回 objectKey
  → fileStorageService.generatePresignedUrl(objectKey, expire)  // 即时签名 URL
  → FileUploadVO { url=签名URL(短期预览), objectKey=存库值 }
```

DB 只存 objectKey（`avatar/202607/uuid.png`），不再存完整 URL。

### 2.2 读取侧（跨服务批量签名）

```
mrb-user.UserServiceImpl.toVOList
  → 收集所有 user.avatar（过滤 http 旧数据）
  → FileFeignClient.batchPresignedUrls(keys)  // 一次 Feign 调用
  → 回填 vo.avatar（命中签名 URL，否则保留原值）
```

mrb-meeting.MeetingRoomServiceImpl 同理处理 image_url。

### 2.3 兼容策略

| 存储值 | 处理 |
|--------|------|
| null/空 | 保留原值（前端占位图） |
| `http` 开头 | 旧一期公开链接，原样返回（FileInternalController 跳过签名） |
| objectKey | 调用 mrb-platform 生成预签名 URL |

### 2.4 内部接口契约

```
POST /file/internal/presigned-urls
Request:  List<String> objectKeys
Response: Result<Map<String, String>>  // objectKey -> 签名URL，http 入参自动跳过
```

## 三、通知域迁移

### 3.1 路径兼容

NotificationController 方法路径保持 `/user/notification/**` 与 `/user/internal/notification/**`，前端调用路径零改动。网关通过细粒度路由 `Path=/api/uc/user/notification/**`（置于 uc-service 之前）分流到 mrb-platform。

### 3.2 Feign 契约

`NotificationFeignClient` 仅改 `@FeignClient(name="mrb-platform")`，方法路径不变。

### 3.3 数据迁移

V1.16 从 `mrb_user.notification` 迁历史数据到 `mrb_platform.notification`；mrb_user 旧表保留过渡，可手动清理。

## 四、字典与系统配置

### 4.1 字典（sys_dict / sys_dict_item）

一对多结构，code 唯一。预留扩展，暂未接入业务。CRUD 路径 `/platform/dict/**`。

### 4.2 系统配置（sys_config）

key-value 结构，config_key 唯一。读取走 Redis 缓存（`mrb:sys:config:{key}`，TTL 30min）。预留扩展，暂未接入业务读取。CRUD 路径 `/platform/config/**`。

## 五、降级策略

| 场景 | 降级 |
|------|------|
| mrb-platform 不可用导致签名失败 | 读取侧 try-catch 降级保留原值，不影响列表/详情查询 |
| mrb-platform 不可用导致通知发送失败 | mrb-meeting 调用方已有降级日志（沿用 030-task8 设计） |
| 本地存储模式 | `generatePresignedUrl` 返回静态 URL，与 COS 行为对齐 |

## 六、遗留事项

1. 腾讯云控制台撤掉 avatar/、room/ 前缀公开读策略，桶回归私有（人工）
2. 端到端验证全链路（头像/会议室图片/上传预览/通知 WebSocket）
3. mrb_user.notification 旧表确认无引用后手动 DROP
