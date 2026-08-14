-- ============================================================
-- V1.21 菜单排序/图标优化 + 补齐缺失菜单（设备管理/我的会议）
-- 1) 一级菜单按使用频率排序：首页 → 会议室 → 我的预约 → 我的会议 → 日历视图 → 通讯录
-- 2) 管理子菜单按 用户→部门→角色→会议室→预约→菜单→设备 排序
-- 3) 图标去重：日历视图 Grid / 通讯录 UserFilled / 部门管理 FolderOpened /
--    预约管理 List / 菜单管理 Menu
-- 4) 补齐 init.sql 缺失的 设备管理(17)/我的会议(18) 及角色菜单关联（幂等）
-- ============================================================

USE `mrb_platform`;

-- ========== 1. 一级菜单排序与图标 ==========
UPDATE `platform_menu` SET `sort_order` = 1, `icon` = 'HomeFilled'   WHERE `id` = 1;
UPDATE `platform_menu` SET `sort_order` = 2, `icon` = 'OfficeBuilding' WHERE `id` = 2;
UPDATE `platform_menu` SET `sort_order` = 3, `icon` = 'Calendar'     WHERE `id` = 3;
UPDATE `platform_menu` SET `sort_order` = 4, `icon` = 'Tickets'      WHERE `id` = 18;
UPDATE `platform_menu` SET `sort_order` = 5, `icon` = 'Grid'         WHERE `id` = 4;
UPDATE `platform_menu` SET `sort_order` = 6, `icon` = 'UserFilled'   WHERE `id` = 5;

-- ========== 2. 管理子菜单排序与图标 ==========
UPDATE `platform_menu` SET `sort_order` = 11, `icon` = 'User'          WHERE `id` = 11;
UPDATE `platform_menu` SET `sort_order` = 12, `icon` = 'FolderOpened'  WHERE `id` = 12;
UPDATE `platform_menu` SET `sort_order` = 13, `icon` = 'Lock'          WHERE `id` = 16;
UPDATE `platform_menu` SET `sort_order` = 14, `icon` = 'OfficeBuilding' WHERE `id` = 13;
UPDATE `platform_menu` SET `sort_order` = 15, `icon` = 'List'          WHERE `id` = 14;
UPDATE `platform_menu` SET `sort_order` = 16, `icon` = 'Menu'          WHERE `id` = 15;
UPDATE `platform_menu` SET `sort_order` = 17, `icon` = 'Box'           WHERE `id` = 17;

-- ========== 3. 补齐缺失菜单（幂等，已存在时覆盖名称/路径/图标/排序） ==========
INSERT INTO `platform_menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`)
VALUES
(17, '设备管理', '/admin/equipments', 'Box', 10, 17, 1, 1),
(18, '我的会议', '/my-meetings', 'Tickets', 0, 4, 1, 1)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `parent_id` = VALUES(`parent_id`),
    `sort_order` = VALUES(`sort_order`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`);

-- ========== 4. 补齐角色菜单关联（幂等） ==========
-- 设备管理仅管理员可见；我的会议管理员与普通用户均可见
INSERT IGNORE INTO `platform_role_menu` (`id`, `role_id`, `menu_id`)
SELECT m.`id` * 100 + r.`id`, r.`id`, m.`id`
FROM `platform_role` r
JOIN `platform_menu` m
WHERE r.`role_code` IN ('ROLE_ADMIN', 'ROLE_USER')
  AND ((r.`role_code` = 'ROLE_ADMIN' AND m.`id` IN (17, 18))
       OR (r.`role_code` = 'ROLE_USER' AND m.`id` = 18));

-- ========== 验证 ==========
-- SELECT `id`, `name`, `icon`, `sort_order` FROM `platform_menu`
--   WHERE `parent_id` = 0 ORDER BY `sort_order`;
-- SELECT `id`, `name`, `icon`, `sort_order` FROM `platform_menu`
--   WHERE `parent_id` = 10 ORDER BY `sort_order`;
