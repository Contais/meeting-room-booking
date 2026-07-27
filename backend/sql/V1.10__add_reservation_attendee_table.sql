-- ============================================================
-- 预约参会人关联表 V1.10（task7：按部门邀请参会人）
-- ============================================================
USE `mrb_meeting`;

CREATE TABLE IF NOT EXISTS `reservation_attendee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reservation_id` BIGINT NOT NULL COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '参会人用户ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '参会状态: 0-待响应, 1-已接受, 2-已拒绝',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservation_attendee` (`reservation_id`, `user_id`, `deleted`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约参会人关联表';
