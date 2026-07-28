-- ============================================================
-- 站内信通知表
-- ============================================================
USE `mrb_user`;

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
