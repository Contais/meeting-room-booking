# Bug 修复汇总 - 回顾性文档

## BUG-001: 删除用户后无法同名新增
- **原因**: uk_username 唯一索引包含已逻辑删除的记录
- **方案**: 去掉 uk_username，改为 uk_phone_active；应用层只查 deleted=0 的记录
- **修复文件**: init.sql, UserServiceImpl

## BUG-002: DTO vs VO 命名不统一
- **原因**: 返回给前端的 UserDTO 不符合 VO 命名规范
- **方案**: UserDTO → UserVO，AuthUserDTO 保持不变（服务间传输）
- **修复文件**: UserVO.java, UserController, UserService, UserServiceImpl

## BUG-003: Token 过期后前端体验差
- **原因**: Axios 拦截器未处理 401 响应
- **方案**: 拦截器捕获 401 → logout → 跳转登录页 → 提示
- **修复文件**: request.ts

## BUG-004: 列表页显示 ID 字段
- **原因**: 表格直接展示数据库 ID
- **方案**: prop="id" 改为 type="index" 显示序号
- **修复文件**: UserManage.vue, RoomManage.vue

## BUG-005: LocalDateTime 反序列化失败
- **原因**: JacksonConfig 使用空格格式，前端发送 ISO 格式（T 分隔符）
- **方案**: JacksonConfig 改用 ISO 格式 yyyy-MM-dd'T'HH:mm:ss
- **修复文件**: JacksonConfig.java

## BUG-006: 代码注释缺失
- **原因**: 实体类和接口缺少注释
- **方案**: 所有实体字段加中文注释，Service/Repository 接口加 Javadoc
- **修复文件**: 实体类、Service 接口、Repository 接口
