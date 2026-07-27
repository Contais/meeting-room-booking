-- V1.11: 预约表新增拒绝原因字段，并更新状态注释
ALTER TABLE `meeting_room_reservation`
    ADD COLUMN `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因（status=3 时填充）' AFTER `status`;

ALTER TABLE `meeting_room_reservation`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认, 1-已确认, 2-已取消, 3-已拒绝';
