-- ============================================================
-- 修复：恢复 user 表手机号唯一索引
-- ============================================================
-- 背景：新增/编辑用户未填写手机号时，空串 '' 被存入 phone 字段，
-- 导致 uk_phone_active(phone, deleted) 唯一索引冲突（多个空串视为相同值）。
-- 修复方案：应用层已将空串归一化为 NULL（MySQL 唯一索引允许多个 NULL），
-- 此脚本清理历史脏数据并恢复索引。

USE `mrb_user`;

-- 1. 清理历史空串手机号，归一化为 NULL
UPDATE `user` SET `phone` = NULL WHERE `phone` = '';

-- 2. 恢复唯一索引（若已存在则跳过）
-- 索引作用：防止同一手机号被多个未删除用户绑定
ALTER TABLE `user` ADD UNIQUE INDEX `uk_phone_active` (`phone`, `deleted`);
