-- ============================================================
-- V1.5: 预约表新增预约编号字段
-- 执行时间: 2026-07-26
-- 说明: 预约创建后自动生成业务编号，格式 B + yyyyMMdd + 6位自增序列
-- ============================================================

ALTER TABLE `mrb_meeting`.`meeting_room_reservation`
    ADD COLUMN `reservation_code` VARCHAR(20) DEFAULT NULL COMMENT '预约编号' AFTER `id`;

ALTER TABLE `mrb_meeting`.`meeting_room_reservation`
    ADD UNIQUE KEY `uk_reservation_code` (`reservation_code`);
