# 039 - 新建 mrb-platform 平台微服务（COS 二期预签名 + 通知/字典/配置迁移）

> 承接 [038-cos-presigned-url](../038-cos-presigned-url/design.md) 的二期方案，新建独立平台微服务，将文件存储、消息通知收敛至 mrb-platform，并落地 COS 预签名 URL 改造。

## 一、需求摘要

| 项 | 内容 |
|----|------|
| 目标 | 新建 mrb-platform 微服务，承载文件存储（含 COS 预签名）、消息通知、字典、系统配置 |
| COS 二期 | 桶回归私有，DB 改存 objectKey，读取侧动态生成预签名 URL |
| 代码迁移 | 文件存储从 mrb-common 迁出；通知模块从 mrb-user 迁出 |
| 粒度 | 最粗：文件存储 + 通知 + 字典 + 系统配置同属一个服务 |

## 二、技术变更清单

### 2.1 新建 mrb-platform 模块

| 文件 | 说明 |
|------|------|
| `backend/mrb-platform/pom.xml` | 依赖 mrb-common + mybatis-plus + mysql + redis + websocket + cos + nacos + feign + rocketmq |
| `PlatformApplication.java` | 启动类，端口 8084，库 mrb_platform |
| `bootstrap.yml` / `application.yml` | nacos 注册 + 数据源 + 文件存储配置（含 `cos.presigned-expire=3600`） |

### 2.2 文件存储域（从 mrb-common 迁入）

| 文件 | 变更 |
|------|------|
| `FileStorageService` | 新增 `generatePresignedUrl(objectKey, expireSeconds)`；`store()` 返回值语义改为 objectKey |
| `CosFileStorageService` | 实现 `generatePresignedUrl`（`cosClient.generatePresignedUrl`）；`store()` 返回 objectKey |
| `LocalFileStorageService` | `generatePresignedUrl` 返回静态 URL（本地无鉴权） |
| `FileStorageProperties` | 新增 `cos.presigned-expire` |
| `FileController` / `LocalFileResourceController` / `FileService` / `FileServiceImpl` / `FileUploadVO` / `FileBizType` | 平移至 platform 包 |
| `FileInternalController`（新） | 暴露 `POST /file/internal/presigned-urls` 批量签名接口供跨服务调用 |

### 2.3 通知域（从 mrb-user 迁入）

| 文件 | 说明 |
|------|------|
| `NotificationController` / `NotificationService(Impl)` / `NotificationRepository` / `Notification` / `NotificationVO` | 平移至 platform 包 |
| `NotificationWebSocketHandler` / `WebSocketAuthInterceptor` / `WebSocketConfig` | WebSocket 实时推送整体迁入 |
| Controller 路径 | 保持 `/user/notification/**` 与 `/user/internal/notification/**` 不变，前端零改动 |

### 2.4 字典 + 系统配置域（新建）

| 文件 | 说明 |
|------|------|
| `SysDict` / `SysDictItem` / `SysDictRepository` / `SysDictItemRepository` / `SysDictService(Impl)` / `SysDictController` | 字典 CRUD，路径 `/platform/dict/**` |
| `SysConfig` / `SysConfigRepository` / `SysConfigService(Impl)` / `SysConfigController` | 系统配置 CRUD + Redis 缓存（前缀 `mrb:sys:config:`），路径 `/platform/config/**` |

### 2.5 网关与聚合

| 文件 | 变更 |
|------|------|
| `backend/pom.xml` | `<module>mrb-platform</module>` 聚合 |
| `mrb-gateway/application.yml` | 新增 `uc-notification-service` 路由（通知分流到 mrb-platform）；`file-service` 改指 mrb-platform；新增 `platform-service` 路由；`ws` 路由改指 mrb-platform |

### 2.6 Feign 客户端

| 文件 | 变更 |
|------|------|
| `mrb-meeting/feign/NotificationFeignClient` | `@FeignClient(name="mrb-platform")`，路径不变 |
| `mrb-meeting/feign/FileFeignClient`（新） | 批量预签名 URL |
| `mrb-user/feign/FileFeignClient`（新） | 批量预签名 URL |

### 2.7 读取侧改造（objectKey → 预签名 URL）

| 文件 | 变更 |
|------|------|
| `mrb-user/UserServiceImpl` | 注入 FileFeignClient，`toVOList`/`toVO` 批量签名 avatar；http 旧数据跳过、失败降级保留原值 |
| `mrb-meeting/MeetingRoomServiceImpl` | 注入 FileFeignClient，`listActiveRooms`/`getRoomDetail`/`listRooms` 批量签名 image_url |

### 2.8 清理

| 文件 | 变更 |
|------|------|
| mrb-common | 删除文件存储全部类；pom 移除 cos_api optional 依赖 |
| mrb-user | 删除通知域全部类；pom 移除 websocket / cos_api 依赖；application.yml 移除 file.storage 与 multipart 配置 |

### 2.9 DB 迁移

| 文件 | 说明 |
|------|------|
| `sql/V1.16__create_platform_schema.sql` | 建 mrb_platform 库 + notification（迁历史数据）+ sys_dict + sys_dict_item + sys_config |
| `sql/V1.17__migrate_avatar_image_to_objectkey.sql` | user.avatar / meeting_room.image_url 剥离 COS 域名前缀转 objectKey；附控制台撤公开读提示 |

## 三、冲突与风险

| 风险 | 应对 |
|------|------|
| 跨服务签名引入 Feign 调用 | 列表页一次批量签名，单次 Feign 调用；失败降级保留原值，不影响主流程 |
| 旧数据（http 完整 URL）兼容 | 读取侧判断 http 开头原样返回；FileInternalController 跳过 http 入参 |
| 通知表数据迁移 | V1.16 从 mrb_user.notification 迁历史数据到 mrb_platform.notification；mrb_user 旧表保留过渡，可手动清理 |
| 桶撤公开读 | 需在腾讯云控制台手动操作（V1.17 脚本附提示），脚本不自动执行 |
| Feign 调用循环依赖 | mrb-platform 不反向依赖 mrb-user/mrb-meeting，无循环 |

## 四、完成校验

- [x] 红线零违规（BigDecimal/Redis 前缀/幂等/异常体系/构造器注入/rollbackFor/script setup/{code,message,data}）
- [x] mvn compile 全模块编译通过
- [x] 分层正确，依赖方向 Controller→Service→Repository→Model
- [x] 变更已追踪（本文件 + design.md）
- [ ] 控制台撤 COS 公开读策略（人工）
- [ ] 端到端验证（头像/会议室图片/上传预览/通知推送）— 待部署后执行
