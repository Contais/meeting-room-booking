# Wiki 知识库治理 - 清除电商模板残留、对齐真实系统

> 清理 `.harness/wiki/` 中与会议预约系统不符的存量内容：
> - `接口协议.md` 原为电商模板（商品/购物车/订单/优惠券），改为真实网关路由与接口清单；
> - `领域术语.md` 残留 Order/OrderItem/Money/Address 等电商概念，且 `{attribute}Cents` 命名与编码红线 1（BigDecimal 元）冲突，全部替换为本系统术语与枚举；
> - `数据模型.md` 仅 2 张旧表（且表名错误），按全部迁移文件重写为完整库表结构。

## 需求摘要

1. 接口协议按 mrb-gateway 路由 + 各模块 Controller 真实端点重写。
2. 领域术语按实体、枚举、状态机、命名约定重写，清除电商残留。
3. 数据模型按 init.sql + V1.1~V1.18 累计的最终表结构重写。

## 技术变更清单

| 文件 | 变更 |
|------|------|
| `.harness/wiki/接口协议.md` | 全量重写：网关路由、鉴权方式、统一响应/错误码、按模块接口清单（auth/user/meeting/platform/内部 Feign）、关键 VO 结构 |
| `.harness/wiki/领域术语.md` | 全量重写：核心业务术语、状态定义（启用/预约/参会人/审批/可见/角色等）、技术术语、命名约定（BigDecimal 元，去除 Cents） |
| `.harness/wiki/数据模型.md` | §2 重写：mrb_user / mrb_meeting / mrb_auth / mrb_platform 四库 15 张表的字段、类型、索引与约束 |

## 冲突与风险

- **init.sql 与迁移脚本不同步（存量问题）**：init.sql 缺少 equipment / room_equipment / reservation_attendee / mrb_platform 系列表，且 meeting_room_reservation 缺 contact_phone、status 注释未含 3-已拒绝；数据模型文档按迁移累计的最终状态编写，init.sql 建议单独治理。
- **contact_phone 列未映射到实体（存量差异）**：V1.1 已加列，`MeetingRoomReservation` 实体无该字段，文档如实标注，建议后续补字段或清理列。
- **网关白名单观察项**：`/api/auth/register` 无对应端点；`/api/file/static/**` 与实际静态资源路径 `/api/platform/file/static/**` 不一致（本地静态资源当前可能被网关要求鉴权）。
- **RoleController 权限观察项**：角色管理接口未标注 `@RequiresRole`，当前仅登录即可操作，建议后续按需收紧。
- **业务模型.md 轻微过时**：提及 Permission 聚合根、单次最小预约时长等非现状概念，本次未改动，建议后续治理。
- 本次仅改 `.harness/wiki/` 文档，不涉及代码与运行行为。

## 验收标准

- `接口协议.md` 中无 商品/购物车/订单/优惠券/库存/SKU 等电商残留。
- `领域术语.md` 中无 Order/OrderItem/Money/Address/优惠券 等电商残留，无 `Cents` 命名约定。
- `数据模型.md` 覆盖全部 15 张表，表名与 SQL 迁移一致，主键/基础字段符合 §1 统一规范。
