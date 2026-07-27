# 菜单管理 - 技术方案设计

## 1. 数据库设计

### 1.1 菜单表 (menu)
```sql
CREATE TABLE IF NOT EXISTS `menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `path` VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID, 0为顶级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 0-隐藏, 1-显示',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';
```

### 1.2 角色菜单关联表 (role_menu)
```sql
CREATE TABLE IF NOT EXISTS `role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色编码',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';
```

### 1.3 默认数据
```sql
-- 默认菜单
INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(1, '首页', '/home', 'HomeFilled', 0, 1, 1, 1),
(2, '会议室', '/meeting/rooms', 'OfficeBuilding', 0, 2, 1, 1),
(3, '我的预约', '/reservation/my', 'Calendar', 0, 3, 1, 1),
(10, '部门管理', '/admin/departments', 'Menu', 0, 10, 1, 1),
(11, '用户管理', '/admin/users', 'User', 0, 11, 1, 1),
(12, '会议室管理', '/admin/rooms', 'OfficeBuilding', 0, 12, 1, 1),
(13, '预约管理', '/admin/reservations', 'Calendar', 0, 13, 1, 1);

-- admin 角色拥有所有菜单
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('admin', 1), ('admin', 2), ('admin', 3),
('admin', 10), ('admin', 11), ('admin', 12), ('admin', 13);

-- user 角色拥有基础菜单
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('user', 1), ('user', 2), ('user', 3);
```

## 2. 后端架构

### 2.1 Entity
- Menu.java: id, name, path, icon, parentId, sortOrder, visible, status, createTime, updateTime, deleted
- RoleMenu.java: id, role, menuId, createTime

### 2.2 Repository
- MenuRepository extends BaseMapper<Menu>
- RoleMenuRepository extends BaseMapper<RoleMenu>

### 2.3 DTO
- MenuCreateDTO: name, path, icon, parentId, sortOrder, visible
- MenuUpdateDTO: id, name, path, icon, parentId, sortOrder, visible

### 2.4 VO
- MenuVO: id, name, path, icon, parentId, sortOrder, visible, status, createTime, children

### 2.5 Service: MenuService.java
| 方法 | 说明 |
|------|------|
| listTree() | 管理端：查询全部菜单树 |
| listByRole(String role) | 用户端：根据角色查询可见菜单树 |
| create(MenuCreateDTO) | 新增菜单 |
| update(MenuUpdateDTO) | 更新菜单 |
| delete(Long id) | 删除菜单（校验无子菜单） |
| saveRoleMenus(String role, List<Long> menuIds) | 保存角色菜单关联 |

### 2.6 Controller: MenuController.java
| 接口 | 方法 | 说明 |
|------|------|------|
| GET /menu/tree | listTree | 管理端菜单树 |
| GET /menu/my | listByRole | 当前用户可见菜单（根据 token 中的角色） |
| POST /menu/admin/create | create | 新增（admin） |
| PUT /menu/admin/update | update | 更新（admin） |
| DELETE /menu/admin/delete/{id} | delete | 删除（admin） |
| PUT /menu/admin/role-menus | saveRoleMenus | 保存角色菜单权限（admin） |

### 2.7 ErrorCode
```java
MENU_NOT_FOUND(1017, "菜单不存在"),
MENU_HAS_CHILDREN(1018, "存在子菜单，不允许删除"),
```

## 3. 前端方案

### 3.1 类型: menu.d.ts
```ts
export interface MenuItem {
  id: number
  name: string
  path: string
  icon: string
  parentId: number
  sortOrder: number
  visible: number
  status: number
  createTime: string
  children?: MenuItem[]
}
```

### 3.2 API: menu.ts
- getMenuTree()
- getMyMenus()
- createMenu(data)
- updateMenu(data)
- deleteMenu(id)
- saveRoleMenus(role, menuIds)

### 3.3 页面: MenuManage.vue
- el-table 树形展示菜单
- 操作列：编辑、删除
- 新增/编辑对话框：名称、路径、图标、父菜单、排序、是否显示
- 角色权限列：显示哪些角色有此菜单权限，点击可编辑

### 3.4 MainLayout.vue 改造
- 移除硬编码菜单
- 调用 getMyMenus() 获取当前用户菜单
- 动态渲染 el-menu-item / el-sub-menu
- 根据 visible 和角色权限控制显示

### 3.5 路由 + 菜单
- 路由：admin/menus
- 菜单：管理员可见
