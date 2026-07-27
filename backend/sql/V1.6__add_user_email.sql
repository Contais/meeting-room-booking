-- ============================================================
-- V1.6 增加用户邮箱字段
-- ============================================================

USE `mrb_user`;

ALTER TABLE `user` ADD COLUMN `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱' AFTER `phone`;
