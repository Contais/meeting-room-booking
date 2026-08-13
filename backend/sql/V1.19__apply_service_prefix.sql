-- ============================================================
-- V1.19 表名服务前缀统一（同库重命名）+ DROP 遗留 notification
-- 说明：仅对仍在原库、不跨库迁移的表做 RENAME。
--       RBAC（menu/role/role_menu）由 V1.20 跨库迁移处理。
-- ============================================================

USE `mrb_user`;
ALTER TABLE `user` RENAME TO `uc_user`;
ALTER TABLE `department` RENAME TO `uc_department`;
DROP TABLE IF EXISTS `notification`;

USE `mrb_meeting`;
ALTER TABLE `equipment` RENAME TO `meeting_equipment`;
ALTER TABLE `room_equipment` RENAME TO `meeting_room_equipment`;
ALTER TABLE `reservation_attendee` RENAME TO `meeting_room_reservation_attendee`;

USE `mrb_platform`;
ALTER TABLE `notification` RENAME TO `platform_notification`;
ALTER TABLE `sys_dict` RENAME TO `platform_dict`;
ALTER TABLE `sys_dict_item` RENAME TO `platform_dict_item`;
ALTER TABLE `sys_config` RENAME TO `platform_config`;
