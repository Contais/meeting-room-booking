# 051 预约编号生成方式修复 - Redis 按天自增序列

> 修复生产环境创建预约报错：`Data truncation: Data too long for column 'reservation_code'`，并将编号生成方式收敛为 Redis 按天自增，恢复可读、连续的编号格式。

## 需求描述与验收标准

1. 预约创建流程不再因 `reservation_code` 超长报错，预约可正常创建并返回编号。
2. 预约编号恢复 `B + yyyyMMdd + 6 位序列` 格式（如 `B20260812000001`），可读且连续。
3. 编号保持全局唯一（沿用唯一索引 `uk_reservation_code`），多实例并发安全。

验收标准：
- 编号固定 15 位，`VARCHAR(20)` 可容纳，无需变更列定义。
- Redis 不可用时返回明确业务错误，不产生未知异常。
- 后端编译通过，8 条红线零违规。

## 背景与根因

- 016 引入预约编号：`B + yyyyMMdd + 6位自增序列（基于主键 id）`，列定义 `VARCHAR(20)`。
- 049 将主键统一切换为 MyBatis-Plus 雪花算法（19 位 Long），`String.format("%06d", id)` 不会截断，编号变为 `B + yyyyMMdd + 19位ID`（28 位），超出 `VARCHAR(20)` 触发 Data too long。
- 直接拼接完整雪花 ID 的编号不可读、不连续，不符合业务编号的展示预期；雪花 ID 末 6 位不保证唯一，截取方案不可行。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `RedisKeyConstant.java` | 新增 `RESERVATION_CODE_SEQ`（`mrb:reservation:code:seq:`） |
| `ReservationServiceImpl.java` | 注入 `StringRedisTemplate`；编号改为 Redis 按天自增（INCR + 48h TTL），Redis 异常统一转 `BusinessException` |
| `backend/sql/init.sql` | 保持 `reservation_code VARCHAR(20)`，注释恢复 6 位序列 |
| `MeetingRoomReservation.java` / `ReservationBriefVO.java` | 更新字段注释 |
| `.harness/wiki/数据模型.md` / `接口协议.md` / `领域术语.md` | 同步编号格式与生成方式 |

## 方案说明

- 采用 Redis `INCR` 按天自增：key 为 `mrb:reservation:code:seq:{yyyyMMdd}`，首次创建时设置 48h TTL；跨实例原子自增，避免并发冲突。
- 编码红线：Redis Key 统一走 `RedisKeyConstant`，前缀为 `mrb:`。
- 该服务定时任务已依赖 Redis（`stringRedisTemplate`），预约创建新增 Redis 读取属既有基础设施，不引入新中间件。

## 冲突与风险

- 预约创建链路新增对 Redis 的依赖；Redis 故障时预约创建失败并返回明确业务错误（不产生未知异常）。
- 若 Redis 数据被清空导致当日序列重置，唯一索引会拦截重复编号（极端运维场景，可接受）。
- 已存在的旧编号（自增 era，15 位）不受影响，无需回填；无需执行数据库迁移。
