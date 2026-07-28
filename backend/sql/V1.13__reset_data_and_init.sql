-- ============================================================
-- 数据重置与初始化：角色编码重构 + 多层级部门 + 200 员工
-- ============================================================
-- 1. 角色编码 admin/user → ROLE_ADMIN/ROLE_USER
-- 2. 清空旧部门/用户(保留 admin/test)/角色数据
-- 3. 初始化多层级部门（4 层 22 个部门）
-- 4. 初始化 200 名员工（80%有手机+邮箱, 8%仅手机, 8%仅邮箱, 4%无联系方式）

USE `mrb_user`;

-- ========== 1. 更新保留用户的角色编码 ==========
UPDATE `user` SET `role` = 'ROLE_ADMIN', `real_name` = '超级管理员' WHERE `username` = 'admin';
UPDATE `user` SET `role` = 'ROLE_USER' WHERE `username` = 'test';

-- ========== 2. 清空旧数据 ==========
-- 删除除 admin/test 外的所有用户
DELETE FROM `user` WHERE `username` NOT IN ('admin', 'test');
-- 清空部门
DELETE FROM `department`;
-- 清空角色及角色菜单关联
DELETE FROM `role`;
DELETE FROM `role_menu`;

-- 重置自增 ID
ALTER TABLE `department` AUTO_INCREMENT = 1;
ALTER TABLE `role` AUTO_INCREMENT = 1;
ALTER TABLE `role_menu` AUTO_INCREMENT = 1;

-- ========== 3. 重新插入角色 ==========
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `is_system`, `sort`) VALUES
('ROLE_ADMIN', '超级管理员', '拥有系统所有权限', 1, 1, 1),
('ROLE_USER', '普通用户', '基础功能权限', 1, 1, 2);

-- 重新插入角色菜单关联
INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('ROLE_ADMIN', 1), ('ROLE_ADMIN', 2), ('ROLE_ADMIN', 3), ('ROLE_ADMIN', 4), ('ROLE_ADMIN', 5),
('ROLE_ADMIN', 10), ('ROLE_ADMIN', 11), ('ROLE_ADMIN', 12), ('ROLE_ADMIN', 13), ('ROLE_ADMIN', 14), ('ROLE_ADMIN', 15), ('ROLE_ADMIN', 16);

INSERT INTO `role_menu` (`role`, `menu_id`) VALUES
('ROLE_USER', 1), ('ROLE_USER', 2), ('ROLE_USER', 3), ('ROLE_USER', 4), ('ROLE_USER', 5);

-- ========== 4. 初始化多层级部门 ==========
-- 星辰科技（总公司）
--   ├── 技术中心
--   │   ├── 研发部（前端组 / 后端组 / 测试组）
--   │   ├── 运维部
--   │   └── 架构组
--   ├── 产品中心
--   │   ├── 产品部
--   │   └── 设计部（UI组 / UX组）
--   ├── 市场中心
--   │   ├── 市场部
--   │   └── 品牌部
--   ├── 人力资源中心
--   │   ├── 招聘部
--   │   └── 培训部
--   └── 财务中心
--       ├── 会计部
--       └── 审计部

INSERT INTO `department` (`id`, `name`, `parent_id`, `sort`, `status`) VALUES
(1,  '星辰科技',     0,  1, 1),
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

-- ========== 5. 批量生成 200 名员工 ==========
-- 分布：1-160 有手机+邮箱, 161-176 仅手机, 177-192 仅邮箱, 193-200 无联系方式

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

        -- 随机部门（1-22，偏向叶子部门 4-8,10,12-13,15-16,18-19,21-22）
        SET v_dept_id = ELT(FLOOR(RAND() * 16) + 1,
            4, 5, 6, 7, 8,         -- 技术中心子部门
            10, 12, 13,            -- 产品中心子部门
            15, 16,                -- 市场中心子部门
            18, 19,                -- 人力资源子部门
            21, 22, 3              -- 财务子部门 + 研发部
        );

        -- 联系方式分布
        IF i <= 160 THEN
            -- 80%：手机 + 邮箱
            SET v_phone = CONCAT('138', LPAD(i + 100, 8, '0'));
            SET v_email = CONCAT(v_username, '@xingchen.tech');
        ELSEIF i <= 176 THEN
            -- 8%：仅手机
            SET v_phone = CONCAT('138', LPAD(i + 100, 8, '0'));
            SET v_email = NULL;
        ELSEIF i <= 192 THEN
            -- 8%：仅邮箱
            SET v_phone = NULL;
            SET v_email = CONCAT(v_username, '@xingchen.tech');
        ELSE
            -- 4%：无联系方式
            SET v_phone = NULL;
            SET v_email = NULL;
        END IF;

        INSERT INTO `user` (`username`, `password`, `phone`, `email`, `real_name`, `role`, `department_id`, `status`)
        VALUES (
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

-- ========== 验证 ==========
-- SELECT COUNT(*) AS total_users FROM `user`;
-- SELECT COUNT(*) AS total_depts FROM `department`;
-- SELECT
--   SUM(CASE WHEN phone IS NOT NULL AND email IS NOT NULL THEN 1 ELSE 0 END) AS both_count,
--   SUM(CASE WHEN phone IS NOT NULL AND email IS NULL THEN 1 ELSE 0 END) AS phone_only,
--   SUM(CASE WHEN phone IS NULL AND email IS NOT NULL THEN 1 ELSE 0 END) AS email_only,
--   SUM(CASE WHEN phone IS NULL AND email IS NULL THEN 1 ELSE 0 END) AS neither
-- FROM `user` WHERE username NOT IN ('admin', 'test');
