-- ============================================================
-- V1.1: 会议室规则配置 + 预约字段扩展
-- 执行时间: 2026-07-22
-- ============================================================

-- 1. 会议室表新增使用规则字段
ALTER TABLE `mrb_meeting`.`meeting_room`
    ADD COLUMN `bookable_start` VARCHAR(5) DEFAULT '08:00' COMMENT '可预约开始时间' AFTER `status`,
    ADD COLUMN `bookable_end` VARCHAR(5) DEFAULT '20:00' COMMENT '可预约结束时间' AFTER `bookable_start`,
    ADD COLUMN `max_duration` INT DEFAULT 480 COMMENT '最大预约时长(分钟)' AFTER `bookable_end`,
    ADD COLUMN `advance_days` INT DEFAULT 7 COMMENT '提前预约天数' AFTER `max_duration`,
    ADD COLUMN `need_approval` TINYINT DEFAULT 0 COMMENT '是否需要审批 0-否 1-是' AFTER `advance_days`;

-- 2. 预约表新增业务字段
ALTER TABLE `mrb_meeting`.`meeting_room_reservation`
    ADD COLUMN `subject` VARCHAR(128) DEFAULT NULL COMMENT '会议主题' AFTER `user_id`,
    ADD COLUMN `attendee_count` INT DEFAULT NULL COMMENT '参会人数' AFTER `subject`,
    ADD COLUMN `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系人手机号' AFTER `attendee_count`,
    ADD COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注' AFTER `contact_phone`;

-- 3. 用户表唯一索引变更（删除 uk_username，新增 uk_phone_active）
ALTER TABLE `mrb_user`.`user` DROP INDEX `uk_username`;
ALTER TABLE `mrb_user`.`user` ADD UNIQUE KEY `uk_phone_active` (`phone`, `deleted`);
