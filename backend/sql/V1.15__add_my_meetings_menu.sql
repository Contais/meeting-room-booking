-- 新增"我的会议"菜单（作为参会人参加的会议）
USE `mrb_user`;

-- 新增我的会议菜单
INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(18, '我的会议', '/my-meetings', 'Tickets', 0, 3, 1, 1);

-- 调整后续菜单排序（我的预约 3→4, 日历视图 4→5, 通讯录 5→6）
UPDATE `menu` SET `sort_order` = 4 WHERE `id` = 3;
UPDATE `menu` SET `sort_order` = 5 WHERE `id` = 4;
UPDATE `menu` SET `sort_order` = 6 WHERE `id` = 5;

-- 为所有角色添加我的会议菜单权限
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('ROLE_ADMIN', 18),
('ROLE_USER', 18);
