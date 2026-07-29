-- ============================================================
-- V1.16 平台服务库初始化
-- ============================================================
-- 1. 新建 mrb_platform 库
-- 2. 通知表 notification 从 mrb_user 迁入 mrb_platform（结构同 V1.14）
-- 3. 新建字典表 sys_dict / sys_dict_item
-- 4. 新建系统配置表 sys_config
--
-- 注意：通知域已整体迁至 mrb-platform 微服务，mrb_user.notification 表
--       迁移完成后可手动 DROP 清理（本脚本不自动删除，保留过渡期容错）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mrb_platform`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `mrb_platform`;

-- ========== 1. 通知表（结构与 mrb_user.notification 一致） ==========
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `type` VARCHAR(32) NOT NULL COMMENT '类型: RESERVATION_CREATED/APPROVED/REJECTED/CANCELLED/SYSTEM',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型 (reservation/user)',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信通知表';

-- 迁移历史通知数据（如 mrb_user.notification 不存在则跳过）
INSERT INTO `mrb_platform`.`notification`
    (`user_id`, `type`, `title`, `content`, `ref_type`, `ref_id`, `is_read`, `create_time`, `update_time`, `deleted`)
SELECT `user_id`, `type`, `title`, `content`, `ref_type`, `ref_id`, `is_read`, `create_time`, `update_time`, `deleted`
FROM `mrb_user`.`notification`
WHERE EXISTS (SELECT 1 FROM information_schema.tables
              WHERE table_schema = 'mrb_user' AND table_name = 'notification');

-- ========== 2. 字典表 ==========
CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典ID',
    `code` VARCHAR(64) NOT NULL COMMENT '字典编码（唯一，如 gender、reservation_status）',
    `name` VARCHAR(64) NOT NULL COMMENT '字典名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典表';

-- ========== 3. 字典项表 ==========
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
    `dict_id` BIGINT NOT NULL COMMENT '所属字典ID',
    `code` VARCHAR(64) NOT NULL COMMENT '字典项编码',
    `label` VARCHAR(128) NOT NULL COMMENT '展示标签',
    `value` VARCHAR(128) NOT NULL COMMENT '字典项值',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_dict_id` (`dict_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项表';

-- ========== 4. 系统配置表 ==========
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置键（唯一，如 file.presigned.expire）',
    `config_value` VARCHAR(512) DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
