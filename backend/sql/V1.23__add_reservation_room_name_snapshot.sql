-- ============================================================
-- V1.23: 预约表增加会议室名称快照
-- 说明：会议室删除/改名后，预约历史仍能展示创建时的会议室名称。
--       init.sql 已同步修改；本脚本用于已部署环境的增量升级与历史数据回填。
-- ============================================================

USE `mrb_meeting`;

ALTER TABLE `meeting_room_reservation`
    ADD COLUMN `room_name` VARCHAR(64) DEFAULT NULL COMMENT '会议室名称快照' AFTER `room_id`;

-- 回填历史预约：即使会议室已逻辑删除，仍通过原始 SQL 关联其名称。
UPDATE `meeting_room_reservation` r
LEFT JOIN `meeting_room` mr ON mr.id = r.room_id
SET r.room_name = mr.name
WHERE r.room_name IS NULL
  AND mr.name IS NOT NULL;
