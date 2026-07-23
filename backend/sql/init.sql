CREATE DATABASE IF NOT EXISTS `mrb_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_meeting` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `mrb_user`;
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password` VARCHAR(128) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `real_name` VARCHAR(64) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'user',
    `status` TINYINT NOT NULL DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_active` (`phone`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', '系统管理员', 'admin', 1),
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138001', '测试用户', 'user', 1);

USE `mrb_auth`;

USE `mrb_meeting`;
CREATE TABLE IF NOT EXISTS `meeting_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `location` VARCHAR(128) DEFAULT NULL,
    `capacity` INT DEFAULT NULL,
    `equipment` VARCHAR(256) DEFAULT NULL,
    `image_url` VARCHAR(512) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `bookable_start` VARCHAR(5) DEFAULT '08:00' COMMENT '可预约开始时间',
    `bookable_end` VARCHAR(5) DEFAULT '20:00' COMMENT '可预约结束时间',
    `max_duration` INT DEFAULT 480 COMMENT '最大预约时长(分钟)',
    `advance_days` INT DEFAULT 7 COMMENT '提前预约天数',
    `need_approval` TINYINT DEFAULT 0 COMMENT '是否需要审批 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `meeting_room_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `subject` VARCHAR(128) DEFAULT NULL,
    `attendee_count` INT DEFAULT NULL,
    `contact_phone` VARCHAR(20) DEFAULT NULL,
    `remark` VARCHAR(512) DEFAULT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `meeting_room` (`name`, `location`, `capacity`, `equipment`, `description`, `status`, `bookable_start`, `bookable_end`, `max_duration`, `advance_days`, `need_approval`) VALUES
('大会议室A', '3楼A301', 20, '投影仪,白板,视频会议系统', '适合部门例会和项目评审', 1, '08:00', '20:00', 480, 7, 0),
('中会议室B', '3楼A302', 10, '投影仪,白板', '适合小组讨论', 1, '08:00', '18:00', 240, 3, 1),
('小会议室C', '3楼A303', 6, '电视屏幕', '适合1对1或小型讨论', 1, '09:00', '18:00', 120, 1, 0);
