# 036 OSS 文件上传 — 技术设计

## 1. 目标与约束

- 统一文件上传接口与前端组件，一次实现覆盖头像 / 会议室图片。
- 存储层可插拔：当前腾讯 COS，未来可换阿里 OSS，业务代码零改动。
- 不引入新微服务；复用现有网关 + mrb-user 承载。
- 文件大小 ≤ 5MB，类型 jpg/jpeg/png/webp。
- 上传需鉴权；图片可被浏览器 `<img>` 直接加载（公开读）。

## 2. 架构决策

### 2.1 为什么把文件接口放在 mrb-common + 经网关统一路由到 mrb-user

- 各服务网关 StripPrefix 不一致（uc=2、meeting=1），共享 FileController 单 mapping 无法同时满足两类路由前缀。
- 文件上传是基础设施能力，不绑定业务域；选定 mrb-user 承载，新增独立网关路由 `/api/file/**`（StripPrefix=1）→ `/file/**`，与各业务服务的路径前缀解耦。
- FileController / FileService / FileStorageService 全部置于 mrb-common，被 mrb-user 自动扫描注册；其他服务（auth/meeting）虽也会注册但无网关路由可达，无害。
- 会议室图片也走 `/api/file/upload`（bizType=ROOM_IMAGE），返回 URL 后由现有 `/api/meeting/room/admin/update` 写入 `image_url`，meeting 服务无需接入存储。

### 2.2 存储抽象层设计

```
FileStorageService (interface)
├── LocalFileStorageService   @ConditionalOnProperty(type=local, matchIfMissing=true)
└── CosFileStorageService     @ConditionalOnClass(COSClient) + @ConditionalOnProperty(type=cos)
```

- 接口方法：`store(byte[], objectKey, contentType) → url`、`delete(objectKey)`、`getAccessUrl(objectKey)`、`type()`。
- Local：写入 `./uploads/{objectKey}`，返回 `/api/file/static/{objectKey}`，由 `LocalFileResourceController` 提供静态读取。
- COS：`putObject` 上传，返回 `https://{bucket}.cos.{region}.myqcloud.com/{objectKey}`（或自定义 domain）。
- `cos_api` 在 mrb-common 为 `optional`，不污染 auth/meeting；mrb-user 显式引入以支持运行期 COS。
- 切换存储：仅改 `file.storage.type`，`@ConditionalOnProperty` 自动选择实现，调用方无感知。
- 扩展阿里 OSS：新增 `OssFileStorageService`（@ConditionalOnClass + type=oss）即可，无需改动 FileService / Controller。

### 2.3 objectKey 规则

`{bizType.path}/{yyyyMM}/{uuid无横线}.{ext小写}`

- 按业务 + 年月分目录，便于归档与清理。
- UUID 避免文件名冲突，且对外不可枚举。

## 3. 鉴权与访问

| 路径 | 鉴权 | 说明 |
|------|------|------|
| POST /api/file/upload | 登录 | 网关 AuthGlobalFilter 校验 JWT |
| DELETE /api/file/delete | 登录 | 同上 |
| GET /api/file/static/** | 公开 | 白名单放行，供 `<img src>` 直读（与 COS 公开读一致） |

- 网关 `spring.codec.max-in-memory-size: 10MB`，避免大文件上传被默认 256KB 缓冲限制拒绝。
- mrb-user `spring.servlet.multipart.max-file-size: 5MB / max-request-size: 10MB`，与业务校验一致。

## 4. 前端设计

### 4.1 FileUpload 组件

- Props：`modelValue`(url, v-model)、`bizType`、`shape`(avatar|card)、`accept`、`maxSize`、`disabled`、`hint`。
- Emits：`update:modelValue`、`change(FileUploadVO|null)`。
- 行为：el-upload 自定义 `:http-request` 调 `uploadFile`；预览态悬浮「重新上传 / 移除」；移除时仅删本次会话上传的文件（防误删既有图）。
- 形态：avatar=圆形 96px；card=矩形 200×130。

### 4.2 头像字段兼容

`user.avatar` 兼容两种格式，前端 `utils/avatar.ts#isAvatarUrl` 判定：
- 图片 URL：`^https?://` 或 `^/api/` 开头 → 渲染 `<img>`。
- 旧图标 JSON `{icon,gradient}` → 渲染图标/首字母 + 渐变背景。

涉及渲染点：MainLayout（顶栏 + 下拉）、ProfileView（banner + 预览）、ContactsView（卡片 + 列表）。

### 4.3 头像上传交互

ProfileView 头像弹窗改为 `el-tabs`：「图标头像」（原有图标+渐变选择）与「上传图片」（FileUpload avatar 形态）。上传成功立即调用 `/api/uc/user/me/profile` 保存 URL，无需二次确认。

## 5. 风险与后续

- 孤儿文件：替换图片不自动删旧文件，后续可加定时任务按 objectKey 时间清理未引用资源。
- COS 私有读：若未来桶改为私有读，需改用预签名 URL（`generatePresignedUrl`），`getAccessUrl` 调整即可。
- 多实例本地存储：local 模式仅适合单实例；生产建议切 COS 或挂载共享卷。
