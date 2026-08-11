-- ============================================================
-- V1.18 数据库统一规范对齐（详见 .harness/changes/049-entity-snowflake-refactor）
-- 1) role_menu 补齐基础字段 update_time / deleted
-- 2) 移除全部主键 AUTO_INCREMENT，主键统一由 MyBatis-Plus 雪花算法生成
-- 说明：init.sql 已同步修改，本脚本用于已部署环境的增量升级。
-- ============================================================

-- ---------- mrb_user ----------
USE `mrb_user`;

ALTER TABLE `role_menu`
    ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是';

ALTER TABLE `user` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '用户ID';
ALTER TABLE `department` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '部门ID';
ALTER TABLE `menu` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '菜单ID';
ALTER TABLE `role` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '角色ID';
ALTER TABLE `role_menu` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT 'ID';

-- 旧通知表（V1.16 已迁至 mrb_platform，如已 DROP 则跳过）
SET @legacy_notification_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = 'mrb_user' AND table_name = 'notification'),
    'ALTER TABLE `mrb_user`.`notification` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT ''通知ID''',
    'SELECT 1'
);
PREPARE legacy_notification_stmt FROM @legacy_notification_sql;
EXECUTE legacy_notification_stmt;
DEALLOCATE PREPARE legacy_notification_stmt;

-- ---------- mrb_meeting ----------
USE `mrb_meeting`;

ALTER TABLE `meeting_room` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '会议室ID';
ALTER TABLE `meeting_room_reservation` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '预约ID';
ALTER TABLE `equipment` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '设备ID';
ALTER TABLE `room_equipment` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '关联ID';
ALTER TABLE `reservation_attendee` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '主键ID';

-- ---------- mrb_platform ----------
USE `mrb_platform`;

ALTER TABLE `notification` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '通知ID';
ALTER TABLE `sys_dict` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '字典ID';
ALTER TABLE `sys_dict_item` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '字典项ID';
ALTER TABLE `sys_config` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '配置ID';
