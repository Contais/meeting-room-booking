# 修复前端雪花ID精度丢失 - id 全链路字符串化

> 后端主键切换为雪花算法后为 19 位 Long，后端已通过 Jackson 统一序列化为字符串；
> 但前端类型声明为 `number`，且多处对 id 执行 `Number()` 转换，导致请求时精度丢失
> （如 `"2087369211579174914"` → `2087369211579175000`），后端按错误 ID 查询失败。

## 根因

1. 所有 TS 类型中 id 类字段声明为 `number`（运行时实际为字符串），类型契约错误。
2. 详情页从路由参数取 id 时执行 `Number(route.params.id)`，19 位字符串转 number 即丢精度。
3. `parentId` 的「是否为顶级(0)」判断也通过 `Number(parentId)` 转换实现。
4. `getRoleColor` 以 `id % N` 取色，字符串 id 下计算结果为 NaN。

## 技术变更清单

### 类型与 API 层（id 统一为 string）

| 文件 | 变更 |
|------|------|
| `types/department.d.ts` / `equipment.d.ts` / `meeting.d.ts` / `menu.d.ts` / `reservation.d.ts` / `user.d.ts` | id / roomId / userId / parentId / departmentId / attendeeUserIds / menuIds 等改为 `string` |
| `api/auth.ts` | LoginVO.userId → string |
| `api/meeting.ts` / `user.ts` / `department.ts` / `menu.ts` / `equipment.ts` / `reservation.ts` / `attendee.ts` / `role.ts` / `notification.ts` | 全部 id 入参签名改为 string，menuIds/userIds/equipmentIds → string[] |

### 视图与组件层（消除运行时的 Number 转换）

| 文件 | 变更 |
|------|------|
| `RoomDetailView` / `admin/RoomDetail` / `admin/UserDetail` / `admin/EquipmentDetail` / `MyReservationDetail` | `Number(route.params.id)` → `String(route.params.id)` |
| `RoomListView` / `MyMeetingsView` / `MyReservations` / `admin/ReservationManage` | `goDetail(id: string)` |
| `admin/RoomManage` / `UserManage` / `DeptManage` / `MenuManage` / `EquipmentManage` | 表单 id/parentId/departmentId/roomIds/equipmentIds → string |
| `DeptManage` / `MenuManage` | 顶级节点判断由 `Number(parentId) === 0` 改为 `parentId === '0'` |
| `RoleManage` | formData.id → string；getRoleColor 改用字符串哈希取色 |
| `BookingDialog` / `ContactsView` | 参会人/部门选择全链路 string：attendeeUserIds、Map/Set 键、UNASSIGNED 哨兵改为字符串 |
| `ScheduleViewV2` / `RoomScheduleView` / `TimeSlotCalendar` / `RoomCalendar` | roomId props 与布局 Map 键改为 string |
| `stores/notification` | `readOne(id: string)` |

## 验证

- `cd frontend && npm run build`（vue-tsc 类型检查 + vite 构建）通过。
- `rg "Number(route.params.id)" frontend/src` 无结果。
- `rg "id: number|roomId: number|userId: number|parentId: number|menuIds: number" frontend/src` 无结果。
- WebSocket 通知推送的 id 已确认由后端 `ObjectMapper`（JacksonConfig Long→String）序列化，前端按 string 消费。

## 冲突与风险

- 纯前端类型与转换修复，不改变后端接口与序列化行为。
- `parentId` 顶级判断统一按字符串 `'0'` 比较；后端仍接受 `parentId=0` 的字符串参数。
- 部门/设备列表按 deptId/roomId 排序仍使用 `Number()` 仅用于相对顺序比较，不参与请求参数，无精度影响。
