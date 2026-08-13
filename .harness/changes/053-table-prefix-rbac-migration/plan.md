# 表名前缀统一 + RBAC 迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将全部表名统一为「服务前缀 + 实体 snake_case」，并把 menu/role/role_menu 从 mrb-user 迁至 mrb-platform，role_menu 统一为 role_id。

**Architecture:** 数据层（@TableName / Mapper / DDL）与代码模块（RBAC 集群）同步调整；跨库数据迁移用 V1.19（同库重命名）+ V1.20（RBAC 建表/迁数/DROP）；前端菜单与角色接口路径从 `/api/uc/*` 切到 `/api/platform/*`。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis-Plus / MySQL；Vue 3 + TypeScript。

---

## 任务总览

1. 非 RBAC 表名前缀（后端）
2. RBAC 后端代码搬移 + role_id 化
3. 前端路径切换 + 删除死代码
4. 存量库迁移脚本 + init.sql + H2 schema
5. wiki / 代码规范同步
6. 全量验证

> 提交信息遵循 Conventional Commits。每完成一个任务提交一次。

---

### Task 1: 非 RBAC 表名前缀（后端 @TableName + Mapper XML）

**Files:**
- Modify: `backend/mrb-user/mrb-user-service/src/main/java/com/meetinghub/user/model/entity/User.java`
- Modify: `backend/mrb-user/mrb-user-service/src/main/java/com/meetinghub/user/model/entity/Department.java`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/java/com/meetinghub/meeting/model/entity/Equipment.java`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/java/com/meetinghub/meeting/model/entity/RoomEquipment.java`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/java/com/meetinghub/meeting/model/entity/ReservationAttendee.java`
- Modify: `backend/mrb-platform/mrb-platform-service/src/main/java/com/meetinghub/platform/model/entity/Notification.java`
- Modify: `backend/mrb-platform/mrb-platform-service/src/main/java/com/meetinghub/platform/model/entity/SysDict.java`
- Modify: `backend/mrb-platform/mrb-platform-service/src/main/java/com/meetinghub/platform/model/entity/SysDictItem.java`
- Modify: `backend/mrb-platform/mrb-platform-service/src/main/java/com/meetinghub/platform/model/entity/SysConfig.java`
- Modify: `backend/mrb-user/mrb-user-service/src/main/resources/mapper/UserRepository.xml`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/resources/mapper/EquipmentRepository.xml`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/resources/mapper/RoomEquipmentRepository.xml`
- Modify: `backend/mrb-meeting/mrb-meeting-service/src/main/resources/mapper/ReservationRepository.xml`

- [ ] **Step 1: 修改 @TableName**

把下列 `@TableName("...")` 值改为新值（其余内容不动）：

| 文件 | 旧值 | 新值 |
|------|------|------|
| User.java | user | uc_user |
| Department.java | department | uc_department |
| Equipment.java | equipment | meeting_equipment |
| RoomEquipment.java | room_equipment | meeting_room_equipment |
| ReservationAttendee.java | reservation_attendee | meeting_room_reservation_attendee |
| platform/Notification.java | notification | platform_notification |
| SysDict.java | sys_dict | platform_dict |
| SysDictItem.java | sys_dict_item | platform_dict_item |
| SysConfig.java | sys_config | platform_config |

- [ ] **Step 2: 修改 Mapper XML 表名**

`UserRepository.xml`：两处 `FROM user` → `FROM uc_user`。

`EquipmentRepository.xml`：`FROM equipment` → `FROM meeting_equipment`。

`RoomEquipmentRepository.xml`：两处 `FROM room_equipment re` → `FROM meeting_room_equipment re`。

`ReservationRepository.xml`：
- `INNER JOIN reservation_attendee ra` → `INNER JOIN meeting_room_reservation_attendee ra`
- `SELECT 1 FROM reservation_attendee ra` → `SELECT 1 FROM meeting_room_reservation_attendee ra`

- [ ] **Step 3: 编译验证**

Run: `cd backend && mvn -o -q -pl mrb-user/mrb-user-service,mrb-meeting/mrb-meeting-service,mrb-platform/mrb-platform-service -am compile`
Expected: BUILD SUCCESS（无输出即成功）。

- [ ] **Step 4: 提交**

```bash
git add backend/mrb-user backend/mrb-meeting backend/mrb-platform
git commit -m "refactor(db): 非 RBAC 表名加服务前缀"
```

---

### Task 2: RBAC 后端代码搬移 + role_id 化

**Files（从 mrb-user-service 迁到 mrb-platform-service，包名 `com.meetinghub.user` → `com.meetinghub.platform`）：**

- Move: `.../user/controller/MenuController.java` → `.../platform/controller/MenuController.java`
- Move: `.../user/controller/RoleController.java` → `.../platform/controller/RoleController.java`
- Move: `.../user/model/entity/Menu.java` → `.../platform/model/entity/Menu.java`
- Move: `.../user/model/entity/Role.java` → `.../platform/model/entity/Role.java`
- Move: `.../user/model/entity/RoleMenu.java` → `.../platform/model/entity/RoleMenu.java`
- Move: `.../user/model/dto/MenuCreateDTO.java` → `.../platform/model/dto/MenuCreateDTO.java`
- Move: `.../user/model/dto/MenuUpdateDTO.java` → `.../platform/model/dto/MenuUpdateDTO.java`
- Move: `.../user/model/dto/RoleCreateDTO.java` → `.../platform/model/dto/RoleCreateDTO.java`
- Move: `.../user/model/dto/RoleUpdateDTO.java` → `.../platform/model/dto/RoleUpdateDTO.java`
- Move: `.../user/model/dto/RoleMenuAssignDTO.java` → `.../platform/model/dto/RoleMenuAssignDTO.java`
- Move: `.../user/model/vo/MenuVO.java` → `.../platform/model/vo/MenuVO.java`
- Move: `.../user/model/vo/RoleVO.java` → `.../platform/model/vo/RoleVO.java`
- Move: `.../user/repository/MenuRepository.java` → `.../platform/repository/MenuRepository.java`
- Move: `.../user/repository/RoleRepository.java` → `.../platform/repository/RoleRepository.java`
- Move: `.../user/repository/RoleMenuRepository.java` → `.../platform/repository/RoleMenuRepository.java`
- Move: `.../user/service/MenuService.java` → `.../platform/service/MenuService.java`
- Move: `.../user/service/RoleService.java` → `.../platform/service/RoleService.java`
- Move: `.../user/service/impl/MenuServiceImpl.java` → `.../platform/service/impl/MenuServiceImpl.java`
- Move: `.../user/service/impl/RoleServiceImpl.java` → `.../platform/service/impl/RoleServiceImpl.java`

- [ ] **Step 1: 机械搬移（19 文件）**

对每个文件：新文件位于 `com/meetinghub/platform/` 对应子包；把 `package com.meetinghub.user.*;` 改为 `package com.meetinghub.platform.*;`；把 `import com.meetinghub.user.` 前缀改为 `import com.meetinghub.platform.`；方法体、字段、注解除下列 Step 2/3 指定改动外保持不变。然后删除 mrb-user-service 中的旧文件。

mrb-platform-service 需新建 `model/dto` 包（放 5 个 DTO）。

- [ ] **Step 2: 实体表名与 RoleMenu 字段**

`Menu.java`：`@TableName("menu")` → `@TableName("platform_menu")`。

`Role.java`：`@TableName("role")` → `@TableName("platform_role")`。

`RoleMenu.java`：`@TableName("role_menu")` → `@TableName("platform_role_menu")`；字段 `private String role;` → `private Long roleId;`。

- [ ] **Step 3: role_id 化 Service**

`MenuServiceImpl.java` 注入 `RoleRepository`，`listByRole` 改为按 `role_code` 解析 id 后按 `role_id` 查询：

```java
private final RoleRepository roleRepository;

@Override
public List<MenuVO> listByRole(String roleCode) {
    Role role = roleRepository.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode)
    );
    if (role == null) return List.of();
    List<RoleMenu> roleMenus = roleMenuRepository.selectList(
            new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, role.getId())
    );
    if (roleMenus.isEmpty()) return List.of();
    List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    List<Menu> menus = list(new LambdaQueryWrapper<Menu>()
            .in(Menu::getId, menuIds)
            .eq(Menu::getStatus, EnableStatusEnum.ENABLED.getCode())
            .eq(Menu::getVisible, VisibleEnum.VISIBLE.getCode())
            .orderByAsc(Menu::getSortOrder));
    return buildTree(menus.stream().map(this::toVO).collect(Collectors.toList()), ROOT_PARENT_ID);
}
```

`RoleServiceImpl.java` 中三处改为按 `role_id`：

```java
// assignMenus
LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(RoleMenu::getRoleId, dto.getRoleId());
roleMenuRepository.delete(wrapper);
for (Long menuId : dto.getMenuIds()) {
    RoleMenu rm = new RoleMenu();
    rm.setRoleId(dto.getRoleId());
    rm.setMenuId(menuId);
    roleMenuRepository.insert(rm);
}

// deleteRole 内删除关联
roleMenuRepository.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));

// getRoleMenuIds
return roleMenuRepository.selectList(
        new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId))
        .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
```

`deleteRole` 中不再需要 `role.getRoleCode()`；`getRoleMenuIds` 不再需要先查 role。

- [ ] **Step 4: 删除 saveRoleMenus 死代码**

`MenuService.java`：删除 `void saveRoleMenus(String role, List<Long> menuIds);`。

`MenuServiceImpl.java`：删除整个 `saveRoleMenus` 方法。

`MenuController.java`：删除 `/admin/role-menus` 方法，并删除不再使用的 `java.util.Map` 导入。

- [ ] **Step 5: 编译验证**

Run: `cd backend && mvn -o -q -pl mrb-user/mrb-user-service,mrb-platform/mrb-platform-service -am compile`
Expected: BUILD SUCCESS。

- [ ] **Step 6: 提交**

```bash
git add -A backend/mrb-user backend/mrb-platform
git commit -m "refactor(rbac): menu/role/role_menu 迁入 platform 并统一 role_id"
```

---

### Task 3: 前端路径切换 + 删除死代码

**Files:**
- Modify: `frontend/src/api/menu.ts`
- Modify: `frontend/src/api/role.ts`

- [ ] **Step 1: menu.ts**

把 `getMenuTree/getMyMenus/createMenu/updateMenu/deleteMenu` 的路径前缀 `/api/uc/menu` 改为 `/api/platform/menu`；删除 `saveRoleMenus` 函数及其 `Promise<Result<void>>` 实现。

- [ ] **Step 2: role.ts**

把 9 个函数的路径前缀 `/api/uc/admin/role` 改为 `/api/platform/admin/role`。

- [ ] **Step 3: 前端构建验证**

Run: `cd frontend && npm run build`
Expected: `vue-tsc` 无类型错误，`vite build` 输出 `✓ built`。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/api/menu.ts frontend/src/api/role.ts
git commit -m "refactor(frontend): 菜单/角色接口切至 platform 路由"
```

---

### Task 4: 存量库迁移脚本 + init.sql + H2 schema

**Files:**
- Create: `backend/sql/V1.19__apply_service_prefix.sql`
- Create: `backend/sql/V1.20__migrate_rbac_to_platform.sql`
- Modify: `backend/sql/init.sql`
- Modify: `backend/mrb-user/mrb-user-service/src/test/resources/schema.sql`

- [ ] **Step 1: V1.19 同库重命名 + DROP 遗留 notification**

```sql
USE `mrb_user`;
ALTER TABLE `user` RENAME TO `uc_user`;
ALTER TABLE `department` RENAME TO `uc_department`;
DROP TABLE IF EXISTS `notification`;

USE `mrb_meeting`;
ALTER TABLE `equipment` RENAME TO `meeting_equipment`;
ALTER TABLE `room_equipment` RENAME TO `meeting_room_equipment`;
ALTER TABLE `reservation_attendee` RENAME TO `meeting_room_reservation_attendee`;

USE `mrb_platform`;
ALTER TABLE `notification` RENAME TO `platform_notification`;
ALTER TABLE `sys_dict` RENAME TO `platform_dict`;
ALTER TABLE `sys_dict_item` RENAME TO `platform_dict_item`;
ALTER TABLE `sys_config` RENAME TO `platform_config`;
```

- [ ] **Step 2: V1.20 RBAC 跨库迁移**

在 mrb_platform 建 `platform_menu`、`platform_role`（结构与原 menu/role 一致）、`platform_role_menu(role_id)`，再迁数据：

```sql
USE `mrb_platform`;

CREATE TABLE IF NOT EXISTS `platform_menu` (
  `id` BIGINT NOT NULL COMMENT '菜单ID',
  `name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID, 0为顶级',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台菜单表';

CREATE TABLE IF NOT EXISTS `platform_role` (
  `id` BIGINT NOT NULL COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `is_system` TINYINT NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色表';

CREATE TABLE IF NOT EXISTS `platform_role_menu` (
  `id` BIGINT NOT NULL COMMENT 'ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色菜单关联表';

INSERT INTO `platform_menu` (`id`,`name`,`path`,`icon`,`parent_id`,`sort_order`,`visible`,`status`,`create_time`,`update_time`,`deleted`)
SELECT `id`,`name`,`path`,`icon`,`parent_id`,`sort_order`,`visible`,`status`,`create_time`,`update_time`,`deleted`
FROM `mrb_user`.`menu`;

INSERT INTO `platform_role` (`id`,`role_code`,`role_name`,`description`,`status`,`is_system`,`sort`,`create_time`,`update_time`,`deleted`)
SELECT `id`,`role_code`,`role_name`,`description`,`status`,`is_system`,`sort`,`create_time`,`update_time`,`deleted`
FROM `mrb_user`.`role`;

INSERT INTO `platform_role_menu` (`id`,`role_id`,`menu_id`,`create_time`,`update_time`,`deleted`)
SELECT rm.`id`, r.`id`, rm.`menu_id`, rm.`create_time`, rm.`update_time`, rm.`deleted`
FROM `mrb_user`.`role_menu` rm
JOIN `mrb_user`.`role` r ON r.`role_code` = rm.`role`;
```

迁移数据校验通过后（见 Step 4），再执行：

```sql
USE `mrb_user`;
DROP TABLE `menu`;
DROP TABLE `role`;
DROP TABLE `role_menu`;
```

- [ ] **Step 3: H2 测试 schema**

`mrb-user-service/src/test/resources/schema.sql`：`"user"` → `"uc_user"`（表名与唯一约束一并改）。

- [ ] **Step 4: 迁移校验 SQL（人工在真实库执行）**

```sql
SELECT COUNT(*) AS menu_cnt FROM mrb_platform.platform_menu;
SELECT COUNT(*) AS role_cnt FROM mrb_platform.platform_role;
SELECT COUNT(*) AS role_menu_cnt FROM mrb_platform.platform_role_menu;
-- 校验 role_menu 全部转换为 role_id 且无孤立：
SELECT COUNT(*) FROM mrb_platform.platform_role_menu rm
LEFT JOIN mrb_platform.platform_role r ON r.id = rm.role_id
WHERE r.id IS NULL;
```

Expected: 前三条与 mrb_user 原表行数一致；最后一条为 0。

- [ ] **Step 5: init.sql 同步**

init.sql 当前已与迁移不同步（缺 equipment/reservation_attendee/platform 表，且 menu/role/role_menu 误放在 mrb_meeting 段）。本次做目标化修正：
- `user` → `uc_user`、`department` → `uc_department`，删除 mrb_user 的 `notification`。
- 将 menu/role/role_menu 从 mrb_meeting 段移出，改为在新增的 `mrb_platform` 段建 `platform_menu`/`platform_role`/`platform_role_menu(role_id)`。
- 在 summary 中记录「init.sql 全面重建（补齐缺失表）为后续单独事项」。

- [ ] **Step 6: 提交**

```bash
git add backend/sql/V1.19__apply_service_prefix.sql backend/sql/V1.20__migrate_rbac_to_platform.sql backend/sql/init.sql backend/mrb-user/mrb-user-service/src/test/resources/schema.sql
git commit -m "feat(db): 服务前缀重命名与 RBAC 跨库迁移脚本"
```

---

### Task 5: wiki / 代码规范同步

**Files:**
- Modify: `.harness/wiki/数据模型.md`
- Modify: `.harness/rules/代码规范.md`

- [ ] **Step 1: 数据模型**

把 §2 中四库表名更新为新表名，RBAC 表移到 mrb_platform 段，role_menu 列改为 role_id。

- [ ] **Step 2: 代码规范**

在数据库规范小节补充：表名 = 服务前缀（uc/meeting/platform）+ 实体 snake_case（单数）；关联表用完整左右表名。

- [ ] **Step 3: 提交**

```bash
git add .harness/wiki/数据模型.md .harness/rules/代码规范.md
git commit -m "docs: 表名前缀与 RBAC 归属规范同步"
```

---

### Task 6: 全量验证

- [ ] **Step 1: 后端全模块编译**

Run: `cd backend && mvn -o -q compile`
Expected: BUILD SUCCESS。

- [ ] **Step 2: 后端单测**

Run: `cd backend && mvn -o -q -pl mrb-user/mrb-user-service -am test`
Expected: 除已知沙箱限制（Mockito/Tomcat）外无新增失败；必要时在正常环境重跑。

- [ ] **Step 3: 前端构建**

Run: `cd frontend && npm run build`
Expected: `✓ built`。

- [ ] **Step 4: 残留检查**

```bash
rg -n "reservation_attendee|\bmenu\b|\brole\b|room_equipment|\bequipment\b" backend --glob '*.java' --glob '*.xml' | rg "TableName|FROM|JOIN" || true
rg -n "/api/uc/menu|/api/uc/admin/role" frontend/src || true
```

Expected: 无按旧表名/旧路径的残留引用。

- [ ] **Step 5: 迁移后真实库冒烟（人工）**

登录 → 侧边栏菜单；菜单管理 / 角色管理 / 用户管理角色下拉；会议室预约与参会人邀请。

---

## 自检记录

- Spec 覆盖：命名规范→Task 1/2/4；RBAC 迁移→Task 2/4；role_id→Task 2/4；前端路径→Task 3；文档→Task 5；验证→Task 6。
- 已知遗留：init.sql 未补齐 equipment/reservation_attendee/platform 全量表（存量问题），在 Task 4 Step 5 记录为后续事项。
