-- ============================================================
-- V1.24: 新增平台知识库（RAG 轻量关键词检索）
-- 1) 知识条目表 platform_kb_entry
-- 2) 管理菜单「知识库管理」及管理员角色关联
-- 3) 预置基础知识条目（预约规则/流程/异常/公告）
-- init.sql 已同步修改；本脚本用于已部署环境的增量升级。
-- ============================================================

USE `mrb_platform`;

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

-- ========== 管理菜单（幂等） ==========
INSERT INTO `platform_menu` (`id`, `name`, `path`, `icon`, `parent_id`, `sort_order`, `visible`, `status`)
VALUES (19, '知识库管理', '/admin/knowledge', 'Collection', 10, 18, 1, 1)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `parent_id` = VALUES(`parent_id`),
    `sort_order` = VALUES(`sort_order`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`);

-- 知识库管理仅管理员可见
INSERT IGNORE INTO `platform_role_menu` (`id`, `role_id`, `menu_id`)
SELECT 21, r.`id`, 19 FROM `platform_role` r WHERE r.`role_code` = 'ROLE_ADMIN';

-- ========== 预置知识条目（幂等，仅首次生效，不覆盖管理员后续修改） ==========
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
