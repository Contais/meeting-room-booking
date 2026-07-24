-- 菜单管理功能
-- 新建菜单表 + 角色菜单关联表

USE `mrb_user`;

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

CREATE TABLE IF NOT EXISTS `role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色编码',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(1, '首页', '/home', 'HomeFilled', 0, 1, 1, 1),
(2, '会议室', '/meeting/rooms', 'OfficeBuilding', 0, 2, 1, 1),
(3, '我的预约', '/reservation/my', 'Calendar', 0, 3, 1, 1),
(10, '系统管理', NULL, 'Setting', 0, 10, 1, 1),
(11, '部门管理', '/admin/departments', 'Menu', 10, 11, 1, 1),
(12, '用户管理', '/admin/users', 'User', 10, 12, 1, 1),
(13, '会议室管理', '/admin/rooms', 'OfficeBuilding', 10, 13, 1, 1),
(14, '预约管理', '/admin/reservations', 'Calendar', 10, 14, 1, 1),
(15, '菜单管理', '/admin/menus', 'Menu', 10, 15, 1, 1);

INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('admin', 1), ('admin', 2), ('admin', 3),
('admin', 10), ('admin', 11), ('admin', 12), ('admin', 13), ('admin', 14), ('admin', 15);

INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('user', 1), ('user', 2), ('user', 3);
