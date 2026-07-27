-- ============================================================
-- 设备管理模块 V1.9
-- 1) equipment 设备主表
-- 2) room_equipment 会议室-设备关联表
-- 3) 菜单初始化
-- ============================================================
USE `mrb_meeting`;

-- 设备主表
CREATE TABLE IF NOT EXISTS `equipment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `code` VARCHAR(50) NOT NULL COMMENT '设备编码',
    `name` VARCHAR(100) NOT NULL COMMENT '设备名称',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '设备分类: 投影仪/白板/电视/音响/视频会议/空调/其他',
    `brand` VARCHAR(64) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `purchase_date` DATE DEFAULT NULL COMMENT '购置日期',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '设备描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_code` (`code`, `deleted`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- 会议室-设备关联表（多对多）
CREATE TABLE IF NOT EXISTS `room_equipment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `room_id` BIGINT NOT NULL COMMENT '会议室ID',
    `equipment_id` BIGINT NOT NULL COMMENT '设备ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_equipment` (`room_id`, `equipment_id`, `deleted`),
    KEY `idx_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室-设备关联表';

-- 初始化示例设备数据
INSERT INTO `equipment` (`code`, `name`, `category`, `brand`, `model`, `status`, `description`) VALUES
('EQ-PROJ-001', '激光投影仪', '投影仪', 'Epson', 'CB-FH52', 1, '高亮度激光投影，支持4K'),
('EQ-WB-001', '电子白板', '白板', 'Maxhub', 'BC65E3A', 1, '65英寸交互式电子白板'),
('EQ-TV-001', '智能电视', '电视', 'Huawei', 'Vision X65', 1, '65英寸4K智能电视'),
('EQ-AUDIO-001', '会议音响', '音响', 'Bose', 'DesignLink', 1, '会议室专用音响系统'),
('EQ-VC-001', '视频会议终端', '视频会议', 'Huawei', 'IdeaHub S2', 1, '支持4K视频会议，含摄像头与麦克风阵列'),
('EQ-AC-001', '中央空调', '空调', 'Midea', 'MDV5', 1, '中央空调系统');

-- 新增"设备管理"菜单（在 mrb_user 库的 menu 表，参考 V1.8 菜单 ID 体系）
USE `mrb_user`;
INSERT INTO `menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(17, '设备管理', '/admin/equipments', 'Box', 10, 17, 1, 1);

-- 为 admin 角色添加设备管理菜单权限
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('admin', 17);
