# 036 OSS 文件上传（统一组件 + 存储抽象层）

## 需求摘要

提供统一的文件上传/预览能力，支撑「用户头像」与「会议室图片」两个场景：
- 后端：灵活的存储抽象层 `FileStorageService`，今天用腾讯 COS，明天可换阿里 OSS，调用方无感知。
- 前端：统一 `FileUpload` 公共组件，方便维护。
- 落地顺序：先头像上传（个人中心），再会议室图片（管理员编辑）。

详细技术方案见 [design.md](./design.md)。

## 技术变更清单

### 后端（mrb-common 共享，由 mrb-user 经网关统一承载）

| 类型 | 文件 | 说明 |
|------|------|------|
| 依赖 | `backend/pom.xml` | 新增 `qcloud-cos.version=5.6.259` 与 dependencyManagement |
| 依赖 | `mrb-common/pom.xml` | 引入 `cos_api`（optional，仅编译期需要） |
| 依赖 | `mrb-user/pom.xml` | 引入 `cos_api`（运行期，COS 实现由 user 服务承载） |
| 枚举 | `common/enums/FileBizType.java` | AVATAR / ROOM_IMAGE，决定存储路径分段 |
| VO | `common/model/vo/FileUploadVO.java` | 上传响应（url/objectKey/bizType/originalName/size） |
| 配置 | `common/config/FileStorageProperties.java` | `file.storage.*` 统一配置（type/local/cos） |
| 接口 | `common/service/FileStorageService.java` | 存储抽象层（store/delete/getAccessUrl/type） |
| 实现 | `common/service/impl/LocalFileStorageService.java` | 本地磁盘实现（默认，matchIfMissing） |
| 实现 | `common/service/impl/CosFileStorageService.java` | 腾讯 COS 实现（@ConditionalOnClass + type=cos） |
| 接口 | `common/service/FileService.java` | 业务层（校验/路径生成/委托存储） |
| 实现 | `common/service/impl/FileServiceImpl.java` | 5MB 限制、jpg/png/webp、路径 `{bizType}/{yyyyMM}/{uuid}.{ext}` |
| 控制器 | `common/controller/FileController.java` | POST /file/upload、DELETE /file/delete |
| 控制器 | `common/controller/LocalFileResourceController.java` | GET /file/static/** 本地静态资源（仅 local 模式） |
| 错误码 | `common/exception/ErrorCode.java` | 新增 1101~1106 文件相关错误码 |
| 配置 | `mrb-user/application.yml` | multipart 限制 5MB/10MB + file.storage 默认 local |
| 配置 | `mrb-user/application-local.yml` | file.storage local + COS 启用模板（注释） |
| 配置 | `mrb-gateway/application.yml` | 新增 `/api/file/**` 路由 → mrb-user（StripPrefix=1）+ codec 缓冲 10MB |
| 鉴权 | `mrb-gateway/.../AuthGlobalFilter.java` | 白名单放行 `/api/file/static/`（公开读，与 COS 一致） |

### 前端

| 类型 | 文件 | 说明 |
|------|------|------|
| 类型 | `types/file.d.ts` | FileBizType / FileUploadVO |
| API | `api/file.ts` | uploadFile / deleteFile |
| 工具 | `utils/avatar.ts` | isAvatarUrl（区分图片 URL 与旧图标 JSON） |
| 组件 | `components/FileUpload.vue` | 统一上传组件（avatar 圆形 / card 矩形，v-model:url） |
| 页面 | `views/user/ProfileView.vue` | 头像弹窗新增「上传图片」Tab；banner 渲染图片头像 |
| 布局 | `layouts/MainLayout.vue` | 顶栏 + 下拉面板头像支持图片 URL 渲染 |
| 页面 | `views/contacts/ContactsView.vue` | 通讯录头像支持图片 URL 渲染 |
| 页面 | `views/admin/RoomManage.vue` | 新增/编辑表单加入「会议室图片」上传字段 |
| 页面 | `views/admin/RoomDetail.vue` | 详情页有图片时展示 |

## 接口协议

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/file/upload` | POST | 登录 | multipart：file + bizType，返回 FileUploadVO |
| `/api/file/delete` | DELETE | 登录 | query：objectKey |
| `/api/file/static/**` | GET | 公开 | 仅 local 模式，浏览器 `<img>` 直读 |

## 冲突与风险

- **存储路由集中**：统一接口由 mrb-user 承载（网关 `/api/file/**` → mrb-user）。会议室图片也走该接口，返回 URL 后由会议室更新接口写入 `image_url`，无需 meeting 服务接入存储。
- **本地存储公开读**：local 模式下 `/api/file/static/**` 放行匿名访问，与 COS 默认公开读行为一致；上传接口仍需鉴权。
- **COS 凭证**：当前默认 local，COS 已完整实现但需用户填入 `secret-id/secret-key/bucket/region` 并把 `type` 改为 `cos`（见 application-local.yml 模板）。
- **头像字段兼容**：`user.avatar` 兼容两种格式——图片 URL（`http(s)://` 或 `/api/` 开头）与旧图标 JSON `{icon,gradient}`，前端通过 `isAvatarUrl` 区分渲染。
- **孤儿文件**：当前仅删除「本次会话上传」的文件；编辑场景替换旧图片不自动删除历史文件（可后续补定时清理）。

## 切换 COS 步骤

1. 在 `mrb-user/src/main/resources/application-local.yml` 取消 COS 配置注释并填入凭证。
2. 把 `file.storage.type` 改为 `cos`。
3. 重启 mrb-user，上传链路自动切到 COS，访问 URL 变为 COS 域名。

## 联调验证

### 构建状态
- 前端 `npm run build`：通过（含 `FileUpload.vue` 组件、`ProfileView.vue` 头像弹窗、`RoomManage.vue` 图片字段、`MainLayout.vue` / `ContactsView.vue` 图片渲染）
- 后端 `mvn -pl mrb-user -am compile` 与 `mvn -pl mrb-gateway -am compile`：均通过

### 端到端验证步骤

1. **启动服务**
   - 启动基础设施：Nacos / Redis / MySQL
   - 启动 `mrb-gateway`（8080）
   - 启动 `mrb-user`（默认 local 存储模式，会在工作目录下创建 `./uploads` 目录）

2. **头像上传（个人中心）**
   - 登录任一账号 → 右上角头像下拉 → 个人中心
   - 点击头像区域 → 弹窗切换到「上传图片」Tab
   - 选择 ≤5MB 的 jpg/png/webp 图片 → 上传成功后头像立即更新
   - 刷新页面 / 切换到通讯录页 → 头像以图片形式渲染（URL 形式）
   - 验证旧图标头像仍能正常显示（兼容性）

3. **会议室图片上传（管理员）**
   - 管理员登录 → 会议室管理 → 新增 / 编辑
   - 表单中「会议室图片」字段上传图片 → 保存
   - 进入会议室详情页 → 顶部展示已上传图片
   - 编辑时替换图片 → 保存 → 详情页图片更新

4. **本地存储访问校验**
   - 上传后浏览器开发者工具 Network 中确认 `<img>` 请求路径为 `/api/file/static/{bizType}/{yyyyMM}/{uuid}.{ext}`
   - 该请求无需登录态（白名单放行），返回 200 + 图片二进制
   - mrb-user 工作目录下 `./uploads/{bizType}/{yyyyMM}/` 存在对应文件

5. **异常分支**
   - 上传 >5MB 文件：前端提示 + 后端返回 FILE_TOO_LARGE
   - 上传非图片类型：前端 accept 限制 + 后端返回 FILE_TYPE_NOT_SUPPORTED
   - 未登录上传：网关返回 401

## 红线自检

- [x] 价格字段 N/A（本任务无价格字段）
- [x] Redis Key 前缀 N/A（未使用 Redis）
- [x] MQ 消费者幂等 N/A（未接入 MQ）
- [x] 异常走 BusinessException 体系（FileServiceImpl / CosFileStorageService 均抛 BusinessException）
- [x] Controller 构造器注入（FileController 使用 @RequiredArgsConstructor）
- [x] @Transactional rollbackFor N/A（文件服务无事务）
- [x] Vue 3 `<script setup>`（FileUpload.vue 等）
- [x] API 响应 `{code, message, data}`（FileController 返回 Result）
