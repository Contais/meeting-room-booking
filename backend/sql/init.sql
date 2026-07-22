-- ============================================================
-- 会议室预约系统 - 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mrb_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_meeting` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 用户中心 (mrb_user)
-- ============================================================
USE `mrb_user`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt哈希）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin-管理员, user-普通用户',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_active` (`phone`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', '系统管理员', 'admin', 1),
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138001', '测试用户', 'user', 1);

-- ============================================================
-- 鉴权中心 (mrb_auth)
-- ============================================================
USE `mrb_auth`;

-- ============================================================
-- 会议室管理 (mrb_meeting)
-- ============================================================
USE `mrb_meeting`;

CREATE TABLE IF NOT EXISTS `meeting_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
    `name` VARCHAR(64) NOT NULL COMMENT '会议室名称',
    `location` VARCHAR(128) DEFAULT NULL COMMENT '位置',
    `capacity` INT DEFAULT NULL COMMENT '容纳人数',
    `equipment` VARCHAR(256) DEFAULT NULL COMMENT '设备设施',
    `image_url` VARCHAR(512) DEFAULT NULL COMMENT '实景图片URL',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室表';

CREATE TABLE IF NOT EXISTS `meeting_room_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `room_id` BIGINT NOT NULL COMMENT '会议室ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `subject` VARCHAR(128) DEFAULT NULL COMMENT '会议主题',
    `attendee_count` INT DEFAULT NULL COMMENT '参会人数',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系人手机号',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认, 1-已确认, 2-已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室预约表';

INSERT INTO `meeting_room` (`name`, `location`, `capacity`, `equipment`, `description`, `status`) VALUES
('大会议室A', '3楼A301', 20, '投影仪,白板,视频会议系统', '适合部门例会和项目评审', 1),
('中会议室B', '3楼A302', 10, '投影仪,白板', '适合小组讨论', 1),
('小会议室C', '3楼A303', 6, '电视屏幕', '适合1对1或小型讨论', 1);
