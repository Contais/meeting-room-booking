-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色：1-是，0-否',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `is_system`, `sort`) VALUES
('admin', '超级管理员', '拥有系统所有权限', 1, 1, 1),
('user', '普通用户', '基础功能权限', 1, 1, 2);

-- 新增通讯录菜单
INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(5, '通讯录', '/contacts', 'User', 0, 5, 1, 1);

-- 新增角色管理菜单
INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(16, '角色管理', '/admin/roles', 'Lock', 10, 16, 1, 1);

-- 为 admin 和 user 角色添加通讯录菜单权限
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('admin', 5),
('user', 5);

-- 为 admin 角色添加角色管理菜单权限
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('admin', 16);
