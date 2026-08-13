-- ============================================================
-- V1.20 RBAC 集群迁入 mrb_platform，并统一 role_menu 为 role_id
-- 1) 在 mrb_platform 建 platform_menu / platform_role / platform_role_menu
-- 2) 迁移 mrb_user.menu / role / role_menu 数据
-- 3) role_menu 按 role_code 关联 role 转换为 role_id
-- 4) 校验通过后再 DROP mrb_user 的 menu / role / role_menu
-- ============================================================

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
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台菜单表';

CREATE TABLE IF NOT EXISTS `platform_role` (
    `id` BIGINT NOT NULL COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色表';

CREATE TABLE IF NOT EXISTS `platform_role_menu` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色菜单关联表';

INSERT INTO `platform_menu`
    (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`, `create_time`, `update_time`, `deleted`)
SELECT `id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`, `create_time`, `update_time`, `deleted`
FROM `mrb_user`.`menu`;

INSERT INTO `platform_role`
    (`id`, `role_code`, `role_name`, `description`, `status`, `is_system`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT `id`, `role_code`, `role_name`, `description`, `status`, `is_system`, `sort`, `create_time`, `update_time`, `deleted`
FROM `mrb_user`.`role`;

INSERT INTO `platform_role_menu`
    (`id`, `role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT rm.`id`, r.`id`, rm.`menu_id`, rm.`create_time`, rm.`update_time`, rm.`deleted`
FROM `mrb_user`.`role_menu` rm
JOIN `mrb_user`.`role` r ON r.`role_code` = rm.`role`;

-- 校验（迁移数据应满足以下条件后，才执行末尾的 DROP）：
-- SELECT COUNT(*) FROM mrb_platform.platform_menu;     -- 应与 mrb_user.menu 一致
-- SELECT COUNT(*) FROM mrb_platform.platform_role;     -- 应与 mrb_user.role 一致
-- SELECT COUNT(*) FROM mrb_platform.platform_role_menu; -- 应与 mrb_user.role_menu 一致
-- SELECT COUNT(*) FROM mrb_platform.platform_role_menu rm
--   LEFT JOIN mrb_platform.platform_role r ON r.id = rm.role_id
--   WHERE r.id IS NULL;                                -- 应为 0

-- ============================================================
-- 校验通过后执行（建议迁移前对 mrb_user.menu/role/role_menu 备份）：
-- ============================================================
-- USE `mrb_user`;
-- DROP TABLE `menu`;
-- DROP TABLE `role`;
-- DROP TABLE `role_menu`;
