# 列表搜索条件修复 + 预约编号生成

## 需求描述
修复所有列表页搜索条件已知 bug，并在预约成功后自动生成预约编号。

### 已知问题清单（用户反馈）
1. 关键字搜索时输入内容 → 点击展开 → 再填相关条件，关键字条件会带过来（参数污染）
2. 部分搜索字段可能未生效
3. 存在遗漏的查询条件（如列表有 A 列但无 A 列过滤）
4. 搜索条件时间交互未做好（只有开始时间）
5. 列表缺少创建时间字段（同时需要放到查询条件过滤）
6. 预约会议室后最好能自动生成预约编号（如 B2027072600001）
7. 可能存在未知 bug

## 验收标准
1. 所有列表页搜索：折叠态只发关键字、展开态只发具体字段，互不污染
2. 所有列表页 `createTime` 范围过滤生效（含树形页 DeptManage/MenuManage 客户端过滤）
3. 会议室管理/预约管理补齐缺失过滤字段（会议室编号、预约编号、预约人等）
4. 时间查询改为 `datetimerange` 区间
5. 预约成功后返回并展示预约编号（B + yyyyMMdd + 6 位序列）
6. 后端编译通过

## 技术变更清单

### 后端
| 文件 | 变更 |
|------|------|
| `V1.5__add_reservation_code.sql` | 新增 `reservation_code` 列 + 唯一索引 |
| `init.sql` | 同步 `reservation_code` 字段 |
| `MeetingRoomReservation.java` | 实体新增 `reservationCode` |
| `ReservationVO.java` | VO 新增 `reservationCode` |
| `ReservationService.java` | `createReservation` 返回 `String`（预约编号） |
| `ReservationServiceImpl.java` | 生成 `B+yyyyMMdd+6位id` 编号并回写 |
| `ReservationController.java` | 返回 `Result<String>` |
| `ReservationRepository.xml` | SELECT/resultMap 包含 `reservation_code`；新增 `createTimeStart`/`createTimeEnd`/`reservationCode`/`username` 等过滤 |
| `ReservationPageQuery.java` | 新增 `createTimeStart`/`createTimeEnd`/`reservationCode`/`username` |
| `RoomPageQuery.java` | 新增 `createTimeStart`/`createTimeEnd` |
| `MeetingRoomServiceImpl.java` | 支持创建时间范围过滤 |
| `UserPageQuery.java` | 新增 `createTimeStart`/`createTimeEnd` |
| `UserServiceImpl.java` | 支持创建时间范围过滤 |

### 前端
| 文件 | 变更 |
|------|------|
| `types/reservation.d.ts` | `Reservation` 增加 `reservationCode`；`ReservationPageQuery` 增加 `createTimeStart/EndTime`/`reservationCode`/`username` |
| `types/meeting.d.ts` | `MeetingRoomPageQuery` 增加创建时间范围等字段 |
| `types/user.d.ts` | `UserPageQuery` 增加创建时间范围 |
| `api/reservation.ts` | `createReservation` 返回 `Result<string>` |
| `BookingDialog.vue` | 预约成功后提示编号 |
| `admin/ReservationManage.vue` | 新增预约编号列、预约人列、创建时间列；搜索修复参数污染；增加创建时间范围、预约编号、预约人过滤 |
| `reservation/MyReservations.vue` | 新增预约编号列、创建时间列；搜索修复参数污染；增加创建时间范围过滤 |
| `admin/RoomManage.vue` | 新增创建时间列、创建时间范围过滤、会议室编号过滤 |
| `admin/UserManage.vue` | 新增创建时间范围过滤 |
| `admin/DeptManage.vue` | 客户端过滤补齐：创建时间范围、关键字递归匹配父子节点 |
| `admin/MenuManage.vue` | 客户端过滤补齐：创建时间范围、关键字递归匹配 |
| `meeting/RoomListView.vue` | 修复 `applyFilter` 空实现；统一搜索交互 |

### 预约编号规则
- 格式：`B` + `yyyyMMdd` + 6 位序列（基于主键 id 自增）
- 示例：`B20270726000001`
- 优势：无外部依赖、天然唯一（受主键唯一约束 + DB unique index 双重保障）

## 冲突与风险
- DB 新增 `reservation_code` 列：需执行 `V1.5__add_reservation_code.sql` 迁移
- 历史数据 `reservation_code` 为 NULL，前端列表展示空值兼容
- `createReservation` 接口契约变更（`Void → String`），前后端同步更新
- DeptManage/MenuManage 树形过滤为客户端实现（数据全量加载后过滤），数据量增大时需评估

## 涉及文件
共 22 个文件改动（含 SQL 迁移脚本 1 个、新增 changes 文档 1 个）。

## 提交信息
`fix(list-search): 修复搜索条件污染、补齐创建时间过滤、生成预约编号`
