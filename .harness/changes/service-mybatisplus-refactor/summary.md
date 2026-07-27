# Service 层接入 MyBatis-Plus IService 接口

## 需求描述
Code Review 发现几乎所有 Service 接口及实现类未继承 MyBatis-Plus 的 `IService<T>` / `ServiceImpl<M, T>`，未能复用 MP 提供的通用 CRUD 能力（save、updateById、removeById、getById、list、page、count 等），与已继承 `BaseMapper<T>` 的 Repository 层不对齐。

## 验收标准
1. 拥有独立实体 + Repository 的 Service 接口继承 `IService<T>`
2. 对应实现类继承 `ServiceImpl<XxxRepository, Entity>` 并继续实现业务接口
3. 保留原有业务方法签名，不破坏现有 Controller 调用
4. 编译通过（mvn compile）

## 技术变更清单

### 修改（mrb-user）
| 文件 | 变更 |
|------|------|
| `service/UserService.java` | `interface UserService extends IService<User>` |
| `service/impl/UserServiceImpl.java` | `extends ServiceImpl<UserRepository, User> implements UserService` |
| `service/DepartmentService.java` | `interface DepartmentService extends IService<Department>` |
| `service/impl/DepartmentServiceImpl.java` | `extends ServiceImpl<DepartmentRepository, Department> implements DepartmentService` |
| `service/MenuService.java` | `interface MenuService extends IService<Menu>` |
| `service/impl/MenuServiceImpl.java` | `extends ServiceImpl<MenuRepository, Menu> implements MenuService` |

### 修改（mrb-meeting）
| 文件 | 变更 |
|------|------|
| `service/MeetingRoomService.java` | `interface MeetingRoomService extends IService<MeetingRoom>` |
| `service/impl/MeetingRoomServiceImpl.java` | `extends ServiceImpl<MeetingRoomRepository, MeetingRoom> implements MeetingRoomService` |
| `service/ReservationService.java` | `interface ReservationService extends IService<MeetingRoomReservation>` |
| `service/impl/ReservationServiceImpl.java` | `extends ServiceImpl<ReservationRepository, MeetingRoomReservation> implements ReservationService` |

### 不变更（说明）
| 文件 | 原因 |
|------|------|
| `HomeService` / `HomeServiceImpl` | 聚合统计服务，无单一实体 CRUD，依赖 MeetingRoom 与 Reservation 两个 Repository |
| `AuthService` / `AuthServiceImpl` | 鉴权服务无直接实体/Repository（通过 Feign 调用用户服务），`AuthRepository` 仅为空占位接口 |

## 设计说明
- `ServiceImpl<M extends BaseMapper<T>, T>` 自带 `@Autowired protected M baseMapper`，与现有 `@RequiredArgsConstructor` 注入的 `private final XxxRepository` 共存（同一 Bean，无冲突）
- 现有业务方法（如 `getUserById`、`createUser`、`toggleStatus`）与 `IService` 的 `getById`、`save`、`updateById` 方法签名不冲突（参数类型/返回值不同），可共存
- 现有代码继续使用 `userRepository.selectById(...)` 等方式，未强制改为 `baseMapper.selectById(...)`，保持改动最小化

## 冲突与风险
- 纯接口继承扩展，无 DB / API / 缓存变更
- 不影响现有 Controller 调用（Service 接口方法签名未变）
- 编译期即可验证

## 提交信息
`refactor(backend): Service 层继承 MyBatis-Plus IService/ServiceImpl 通用接口`
