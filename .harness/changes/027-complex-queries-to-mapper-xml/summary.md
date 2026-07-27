# 复杂列表查询下沉到 mapper.xml

## 需求描述

出于可读性与可维护性考虑，将 Service 层中部分「复杂列表查询」（多条件动态拼接、OR 关键字、JOIN 等）从 `LambdaQueryWrapper` 下沉到对应的 `mapper.xml`。

## 现状

- 仅 `mrb-meeting` 存在 mapper.xml：`ReservationRepository.xml`（`selectMyPage` / `selectAllPage` 已下沉，含公共 `commonFilters` 片段）
- `mrb-user` 无任何 mapper.xml，所有查询均用 `LambdaQueryWrapper` 写在 ServiceImpl
- mapper-locations 配置：`classpath:mapper/**/*.xml`（各服务均已配置）

## 候选查询评估

| Service 方法 | 复杂度 | 是否 JOIN | 评估 | 建议 |
|-------------|--------|----------|------|------|
| `UserServiceImpl.listUsers` | 中（keyword OR username/realName + 6 动态条件） | 否（单表 user） | 单表多条件，Wrapper 尚可读，但 OR + 多条件下沉后更清晰 | ✅ 建议下沉 |
| `UserServiceImpl.listContacts` | 中（4 字段 OR + 部门过滤 + 排序） | 否 | 同上，OR 较多 | ✅ 建议下沉 |
| `MeetingRoomServiceImpl.listRooms` | 高（keyword OR name/location + 10 动态条件） | 否（单表） | 条件最多，Wrapper 较长 | ✅ 建议下沉 |
| `ReservationServiceImpl.listByRoomAndDate` | 低（3 条件 + between） | 否 | 简单，无需下沉 | ❌ 保留 |
| `ReservationServiceImpl.getSchedule` | 中（两表内存聚合） | 否（内存 JOIN） | 逻辑在内存聚合，不适合 SQL 下沉 | ❌ 保留 |
| `UserServiceImpl.toVO` 逐个查部门 | N+1 | — | 非列表查询，但存在 N+1 隐患 | ⚠️ 顺带优化（批量查部门） |

## 验收标准

| AC | 描述 |
|----|------|
| AC-1 | `listUsers` / `listContacts` / `listRooms` 改为调用 Repository 自定义方法，SQL 写在对应 mapper.xml |
| AC-2 | mapper.xml 使用 `<sql>` 片段复用公共条件，使用 resultMap 映射 |
| AC-3 | 行为与原 Wrapper 完全一致（分页、排序、过滤结果相同） |
| AC-4 | `mrb-user` 新增 `resources/mapper/UserRepository.xml` |
| AC-5 | ServiceImpl 不再保留对应 Wrapper 拼接逻辑 |
| AC-6 | 红线：Repository 仅做数据访问，不写业务逻辑（符合工程结构规范） |

## 技术变更清单

| 类型 | 文件 | 说明 |
|------|------|------|
| 新增 | `mrb-user/.../resources/mapper/UserRepository.xml` | `selectUserPage`、`selectContacts` |
| 新增 | `mrb-meeting/.../resources/mapper/MeetingRoomRepository.xml` | `selectRoomPage` |
| 修改 | `UserRepository.java` | 新增 `selectUserPage`、`selectContacts` 方法签名 |
| 修改 | `MeetingRoomRepository.java` | 新增 `selectRoomPage` 方法签名 |
| 修改 | `UserServiceImpl.java` | `listUsers` / `listContacts` 改调 Repository |
| 修改 | `MeetingRoomServiceImpl.java` | `listRooms` 改调 Repository |
| 修改 | `UserServiceImpl.toVO` | 批量查询部门名，消除 N+1（可选） |

## 业务影响范围

- 影响模块：用户列表、通讯录、会议室列表（查询性能与结果一致性）
- 用户角色：管理员（用户/会议室列表）、所有用户（通讯录）

## 冲突与风险

- 风险1：下沉后需保证动态条件与原 Wrapper 等价，尤其 `deleted` 逻辑删除、`status` 过滤、`keyword` OR 括号分组
- 风险2：`UserPageQuery` / `RoomPageQuery` 字段需与 XML `#{query.xxx}` 一一对应
- 风险3：分页插件（MyBatis-Plus `IPage`）需正确传入，XML 中不写 LIMIT
- 红线检查：Repository 不写业务逻辑 ✅；事务仍在 Service 层 ✅

## 任务拆分建议

1. `UserRepository.xml` + `listUsers` 下沉 + 单测验证
2. `UserRepository.xml` + `listContacts` 下沉 + 验证
3. `MeetingRoomRepository.xml` + `listRooms` 下沉 + 验证
4. （可选）`toVO` 批量查部门消除 N+1
