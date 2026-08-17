-- ============================================================
-- V1.22: 会议室增加单次最小预约时长
-- 说明：补齐业务模型“单次最小/最大预约时长”中缺失的 min_duration，
--       init.sql 已同步修改；本脚本用于已部署环境的增量升级。
-- ============================================================

USE `mrb_meeting`;

ALTER TABLE `meeting_room`
    ADD COLUMN `min_duration` INT DEFAULT 0 COMMENT '单次最小预约时长(分钟)，0表示不限制' AFTER `bookable_end`;
