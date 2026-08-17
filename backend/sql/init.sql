-- ============================================================
-- 会议室预约系统 - 数据库初始化脚本（最新版）
-- 用于全新环境初始化；已有环境请按 V1.x__*.sql 增量迁移
-- 表名规范：服务前缀（uc/meeting/platform）+ 实体 snake_case（单数）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mrb_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_meeting` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `mrb_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 用户中心 (mrb_user)
-- ============================================================
USE `mrb_user`;

CREATE TABLE IF NOT EXISTS `uc_user` (
    `id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt哈希）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像 objectKey',
    `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
    `role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色编码: ROLE_ADMIN/ROLE_USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_active` (`phone`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `uc_department` (
    `id` BIGINT NOT NULL COMMENT '部门ID',
    `name` VARCHAR(64) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父部门ID, 0为顶级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

INSERT INTO `uc_user` (`id`, `username`, `password`, `phone`, `real_name`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', '超级管理员', 'ROLE_ADMIN', 1),
(2, 'test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138001', '测试用户', 'ROLE_USER', 1);

-- ============================================================
-- 多层级部门种子数据（合并自 V1.13，适配雪花主键：显式 id）
-- ============================================================
INSERT INTO `uc_department` (`id`, `name`, `parent_id`, `sort_order`, `status`) VALUES
(1,  '廿一克有限公司',     0,  1, 1),
(2,  '技术中心',     1,  1, 1),
(3,  '研发部',       2,  1, 1),
(4,  '前端组',       3,  1, 1),
(5,  '后端组',       3,  2, 1),
(6,  '测试组',       3,  3, 1),
(7,  '运维部',       2,  2, 1),
(8,  '架构组',       2,  3, 1),
(9,  '产品中心',     1,  2, 1),
(10, '产品部',       9,  1, 1),
(11, '设计部',       9,  2, 1),
(12, 'UI组',        11, 1, 1),
(13, 'UX组',        11, 2, 1),
(14, '市场中心',     1,  3, 1),
(15, '市场部',      14,  1, 1),
(16, '品牌部',      14,  2, 1),
(17, '人力资源中心',  1,  4, 1),
(18, '招聘部',      17,  1, 1),
(19, '培训部',      17,  2, 1),
(20, '财务中心',     1,  5, 1),
(21, '会计部',      20,  1, 1),
(22, '审计部',      20,  2, 1);

-- ============================================================
-- 200 名员工种子数据（合并自 V1.13）
-- 雪花主键无自增，INSERT 必须显式 id；admin=1、test=2，员工从 3 开始
-- 分布：1-160 有手机+邮箱, 161-176 仅手机, 177-192 仅邮箱, 193-200 无联系方式
-- ============================================================
DELIMITER $$
DROP PROCEDURE IF EXISTS generate_employees$$
CREATE PROCEDURE generate_employees()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_username VARCHAR(50);
    DECLARE v_real_name VARCHAR(64);
    DECLARE v_phone VARCHAR(20);
    DECLARE v_email VARCHAR(128);
    DECLARE v_dept_id BIGINT;
    DECLARE v_surname VARCHAR(10);
    DECLARE v_given1 VARCHAR(10);
    DECLARE v_given2 VARCHAR(10);

    WHILE i <= 200 DO
        -- 用户名
        SET v_username = CONCAT('emp', LPAD(i, 3, '0'));

        -- 随机中文姓名
        SET v_surname = ELT(FLOOR(RAND() * 20) + 1,
            '王','李','张','刘','陈','杨','赵','黄','周','吴',
            '徐','孙','胡','朱','高','林','何','郭','马','罗');
        SET v_given1 = ELT(FLOOR(RAND() * 30) + 1,
            '伟','芳','娜','敏','静','丽','强','磊','军','洋',
            '勇','艳','杰','娟','涛','明','超','霞','平','刚',
            '萍','鹏','华','斌','辉','龙','胜','良','波','飞');
        -- 30% 概率双字名
        IF RAND() < 0.3 THEN
            SET v_given2 = ELT(FLOOR(RAND() * 30) + 1,
                '伟','芳','娜','敏','静','丽','强','磊','军','洋',
                '勇','艳','杰','娟','涛','明','超','霞','平','刚',
                '萍','鹏','华','斌','辉','龙','胜','良','波','飞');
            SET v_real_name = CONCAT(v_surname, v_given1, v_given2);
        ELSE
            SET v_real_name = CONCAT(v_surname, v_given1);
        END IF;

        -- 随机部门（偏向叶子部门）
        SET v_dept_id = ELT(FLOOR(RAND() * 16) + 1,
            4, 5, 6, 7, 8,         -- 技术中心子部门
            10, 12, 13,            -- 产品中心子部门
            15, 16,                -- 市场中心子部门
            18, 19,                -- 人力资源子部门
            21, 22, 3              -- 财务子部门 + 研发部
        );

        -- 联系方式分布
        IF i <= 160 THEN
            SET v_phone = CONCAT('138', LPAD(i + 100, 8, '0'));
            SET v_email = CONCAT(v_username, '@21g.com');
        ELSEIF i <= 176 THEN
            SET v_phone = CONCAT('138', LPAD(i + 100, 8, '0'));
            SET v_email = NULL;
        ELSEIF i <= 192 THEN
            SET v_phone = NULL;
            SET v_email = CONCAT(v_username, '@21g.com');
        ELSE
            SET v_phone = NULL;
            SET v_email = NULL;
        END IF;

        INSERT INTO `uc_user` (`id`, `username`, `password`, `phone`, `email`, `real_name`, `role`, `department_id`, `status`)
        VALUES (
            i + 2,
            v_username,
            '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
            v_phone,
            v_email,
            v_real_name,
            'ROLE_USER',
            v_dept_id,
            1
        );

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL generate_employees();
DROP PROCEDURE IF EXISTS generate_employees;

-- ============================================================
-- 鉴权中心 (mrb_auth)
-- ============================================================
USE `mrb_auth`;
-- Token 存储在 Redis 中，无独立表

-- ============================================================
-- 会议室中心 (mrb_meeting)
-- ============================================================
USE `mrb_meeting`;

CREATE TABLE IF NOT EXISTS `meeting_room` (
    `id` BIGINT NOT NULL COMMENT '会议室ID',
    `name` VARCHAR(64) NOT NULL COMMENT '会议室名称',
    `location` VARCHAR(128) DEFAULT NULL COMMENT '位置',
    `capacity` INT DEFAULT NULL COMMENT '容纳人数',
    `equipment` VARCHAR(256) DEFAULT NULL COMMENT '设备设施',
    `image_url` VARCHAR(512) DEFAULT NULL COMMENT '实景图片 objectKey',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `bookable_start` VARCHAR(5) DEFAULT '08:00' COMMENT '可预约开始时间',
    `bookable_end` VARCHAR(5) DEFAULT '20:00' COMMENT '可预约结束时间',
    `min_duration` INT DEFAULT 0 COMMENT '单次最小预约时长(分钟)，0表示不限制',
    `max_duration` INT DEFAULT 480 COMMENT '最大预约时长(分钟)',
    `advance_days` INT DEFAULT 7 COMMENT '提前预约天数',
    `need_approval` TINYINT DEFAULT 0 COMMENT '是否需要审批 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室表';

CREATE TABLE IF NOT EXISTS `meeting_room_reservation` (
    `id` BIGINT NOT NULL COMMENT '预约ID',
    `reservation_code` VARCHAR(20) DEFAULT NULL COMMENT '预约编号: B + yyyyMMdd + 6位序列',
    `room_id` BIGINT NOT NULL COMMENT '会议室ID',
    `user_id` BIGINT NOT NULL COMMENT '预约用户ID',
    `subject` VARCHAR(128) DEFAULT NULL COMMENT '会议主题',
    `attendee_count` INT DEFAULT NULL COMMENT '参会人数（由参会人列表派生）',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系人手机号',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认, 1-已确认, 2-已取消, 3-已拒绝',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因（status=3 时填充）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservation_code` (`reservation_code`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室预约表';

CREATE TABLE IF NOT EXISTS `meeting_equipment` (
    `id` BIGINT NOT NULL COMMENT '设备ID',
    `code` VARCHAR(50) NOT NULL COMMENT '设备编码',
    `name` VARCHAR(100) NOT NULL COMMENT '设备名称',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '设备分类',
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

CREATE TABLE IF NOT EXISTS `meeting_room_equipment` (
    `id` BIGINT NOT NULL COMMENT '关联ID',
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

CREATE TABLE IF NOT EXISTS `meeting_room_reservation_attendee` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `reservation_id` BIGINT NOT NULL COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '参会人用户ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '参会状态: 0-待查阅, 1-已接受, 2-已拒绝',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservation_attendee` (`reservation_id`, `user_id`, `deleted`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约参会人关联表';

INSERT INTO `meeting_room` (`id`, `name`, `location`, `capacity`, `equipment`, `description`, `status`, `bookable_start`, `bookable_end`, `min_duration`, `max_duration`, `advance_days`, `need_approval`) VALUES
(1, '大会议室A', '3楼A301', 20, '投影仪,白板,视频会议系统', '适合部门例会和项目评审', 1, '08:00', '20:00', 0, 480, 7, 0),
(2, '中会议室B', '3楼A302', 10, '投影仪,白板', '适合小组讨论', 1, '08:00', '18:00', 0, 240, 3, 1),
(3, '小会议室C', '3楼A303', 6, '电视屏幕', '适合1对1或小型讨论', 1, '09:00', '18:00', 0, 120, 1, 0);

-- ============================================================
-- 平台中心 (mrb_platform)
-- ============================================================
USE `mrb_platform`;

CREATE TABLE IF NOT EXISTS `platform_notification` (
    `id` BIGINT NOT NULL COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收人ID',
    `type` VARCHAR(32) NOT NULL COMMENT '类型: RESERVATION_CREATED/APPROVED/REJECTED/CANCELLED/SYSTEM',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型 (reservation/user)',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信通知表';

CREATE TABLE IF NOT EXISTS `platform_dict` (
    `id` BIGINT NOT NULL COMMENT '字典ID',
    `code` VARCHAR(64) NOT NULL COMMENT '字典编码（唯一）',
    `name` VARCHAR(64) NOT NULL COMMENT '字典名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典表';

CREATE TABLE IF NOT EXISTS `platform_dict_item` (
    `id` BIGINT NOT NULL COMMENT '字典项ID',
    `dict_id` BIGINT NOT NULL COMMENT '所属字典ID',
    `code` VARCHAR(64) NOT NULL COMMENT '字典项编码',
    `label` VARCHAR(128) NOT NULL COMMENT '展示标签',
    `value` VARCHAR(128) NOT NULL COMMENT '字典项值',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_dict_id` (`dict_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项表';

CREATE TABLE IF NOT EXISTS `platform_config` (
    `id` BIGINT NOT NULL COMMENT '配置ID',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置键（唯一）',
    `config_value` VARCHAR(512) DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS `platform_kb_entry` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花算法）',
    `category` VARCHAR(32) NOT NULL COMMENT '分类编码',
    `title` VARCHAR(128) NOT NULL COMMENT '条目标题/来源',
    `question` VARCHAR(512) NOT NULL COMMENT '常见问法/问题',
    `answer` TEXT NOT NULL COMMENT '答案内容',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_status` (`category`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台知识库条目表';

CREATE TABLE IF NOT EXISTS `platform_menu` (
    `id` BIGINT NOT NULL COMMENT '菜单ID',
    `name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
    `path` VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    `icon` VARCHAR(64) DEFAULT NULL COMMENT '图标名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID, 0为顶级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 0-隐藏, 1-显示',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台菜单表';

CREATE TABLE IF NOT EXISTS `platform_role` (
    `id` BIGINT NOT NULL COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统角色: 1-是, 0-否',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色表';

CREATE TABLE IF NOT EXISTS `platform_role_menu` (
    `id` BIGINT NOT NULL COMMENT 'ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台角色菜单关联表';

INSERT INTO `platform_menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`) VALUES
(1, '首页', '/home', 'HomeFilled', 0, 1, 1, 1),
(2, '会议室', '/meeting/rooms', 'OfficeBuilding', 0, 2, 1, 1),
(3, '我的预约', '/reservation/my', 'Calendar', 0, 3, 1, 1),
(4, '日历视图', '/schedule', 'Grid', 0, 5, 1, 1),
(5, '通讯录', '/contacts', 'UserFilled', 0, 6, 1, 1),
(10, '系统管理', NULL, 'Setting', 0, 10, 1, 1),
(11, '用户管理', '/admin/users', 'User', 10, 11, 1, 1),
(12, '部门管理', '/admin/departments', 'FolderOpened', 10, 12, 1, 1),
(13, '会议室管理', '/admin/rooms', 'OfficeBuilding', 10, 14, 1, 1),
(14, '预约管理', '/admin/reservations', 'List', 10, 15, 1, 1),
(15, '菜单管理', '/admin/menus', 'Menu', 10, 16, 1, 1),
(16, '角色管理', '/admin/roles', 'Lock', 10, 13, 1, 1),
(17, '设备管理', '/admin/equipments', 'Box', 10, 17, 1, 1),
(18, '我的会议', '/my-meetings', 'Tickets', 0, 4, 1, 1),
(19, '知识库管理', '/admin/knowledge', 'Collection', 10, 18, 1, 1);

INSERT INTO `platform_role` (`id`, `role_code`, `role_name`, `description`, `status`, `is_system`, `sort`) VALUES
(1, 'ROLE_ADMIN', '超级管理员', '拥有系统所有权限', 1, 1, 1),
(2, 'ROLE_USER', '普通用户', '基础功能权限', 1, 1, 2);

-- 雪花主键表无自增，角色菜单关联必须显式指定 id
INSERT INTO `platform_role_menu` (`id`, `role_id`, `menu_id`) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5),
(6, 1, 10), (7, 1, 11), (8, 1, 12), (9, 1, 13), (10, 1, 14), (11, 1, 15), (12, 1, 16), (13, 1, 17), (14, 1, 18), (15, 1, 19);

INSERT INTO `platform_role_menu` (`id`, `role_id`, `menu_id`) VALUES
(15, 2, 1), (16, 2, 2), (17, 2, 3), (18, 2, 4), (19, 2, 5), (20, 2, 18);

INSERT IGNORE INTO `platform_kb_entry` (`id`, `category`, `title`, `question`, `answer`, `tags`, `sort`, `status`) VALUES
(1, 'RULES', '预约规则·提前预约天数', '我可以提前几天预约？', '一般情况下可提前 7 天预约会议室；不同会议室可能配置不同，具体以预约页面可选择的日期范围为准。', '提前,预约,天数', 1, 1),
(2, 'RULES', '预约规则·单次预约时长', '最多能约多久？', '单次预约时长有限制，通常不超过 8 小时；部分会议室可能更短，提交预约时系统会自动校验。', '时长,上限,预约', 2, 1),
(3, 'RULES', '预约规则·最短预约时长', '预约有最短时长限制吗？', '部分会议室设置了单次最短预约时长，预约时需满足该下限，否则无法提交。', '最短,时长', 3, 1),
(4, 'RULES', '预约规则·可预约时段', '每天什么时间可以预约会议室？', '会议室每日可预约时段通常为 08:00–20:00，具体以各会议室设置为准。', '时段,营业时间', 4, 1),
(5, 'FLOW', '预约流程·邀请参会人', '预约之后怎么邀请同事？', '预约成功后进入「我的预约」，找到对应预约进入详情，点击「邀请参会人」，可按部门或按人选择，被邀请人会收到通知。', '邀请,参会人,同事', 1, 1),
(6, 'FLOW', '预约流程·取消预约', '怎么取消预约？', '在「我的预约」找到对应预约，进入详情点击「取消预约」并确认即可。', '取消,退订', 2, 1),
(7, 'FLOW', '预约流程·修改预约', '预约错了时间怎么改？', '系统不支持直接修改预约，请先取消原预约，再按新时间重新预约。', '修改,改期', 3, 1),
(8, 'FLOW', '预约流程·查看我的会议', '在哪里查看我参与的会议？', '点击左侧「我的会议」可查看你创建或参与的会议列表。', '我的会议,查看', 4, 1),
(9, 'EXCEPTION', '异常处理·预约被拒', '预约被拒绝了怎么办？', '进入「我的预约」查看该预约的拒绝原因，确认后调整时间或会议室重新预约。', '被拒,拒绝,审批', 1, 1),
(10, 'EXCEPTION', '异常处理·时段冲突', '预约提示时段已被占用怎么办？', '说明所选时段与其他预约冲突，请更换时间或会议室后重新提交。', '冲突,占用', 2, 1),
(11, 'EXCEPTION', '异常处理·未收到通知', '预约后没收到通知怎么办？', '请点击右上角消息通知图标查看站内信，或刷新页面查看预约状态。', '通知,消息', 3, 1),
(12, 'EXCEPTION', '异常处理·会议室不可用', '会议室显示不可预约是什么原因？', '该会议室可能已被管理员停用或维护，请选择其他会议室。', '不可用,停用', 4, 1),
(13, 'ANNOUNCEMENT', '运营公告·节假日安排', '节假日期间会议室还能约吗？', '节假日期间会议室预约政策以管理员发布的公告为准，请留意首页或消息通知中的公告内容。', '节假日,国庆,公告', 1, 1),
(14, 'ANNOUNCEMENT', '运营公告·系统维护', '系统维护期间能预约吗？', '系统维护期间可能短暂无法预约，请稍后重试。', '维护,升级', 2, 1);
