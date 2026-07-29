-- ============================================================
-- V1.17 avatar / image 字段：完整 URL → objectKey
-- ============================================================
-- COS 二期：DB 改存 objectKey（相对路径），读取侧动态生成预签名 URL。
-- 本脚本剥离一期「前缀公开读」方案遗留的完整 COS URL 域名前缀。
--
-- 兼容策略：读取侧 FileFeignClient 对以 http 开头的字段原样返回，
--          故未迁移的旧数据仍可正常访问；本脚本仅做数据归一化。
-- ============================================================

-- COS 桶默认域名前缀（按实际桶名/地域调整）
-- https://{bucket}.cos.{region}.myqcloud.com/

-- ========== 1. 用户头像 avatar ==========
USE `mrb_user`;
UPDATE `user`
SET `avatar` = REPLACE(`avatar`, 'https://mrb-1310160539.cos.ap-guangzhou.myqcloud.com/', '')
WHERE `avatar` LIKE 'https://%.myqcloud.com/%';

-- ========== 2. 会议室图片 image_url ==========
USE `mrb_meeting`;
UPDATE `meeting_room`
SET `image_url` = REPLACE(`image_url`, 'https://mrb-1310160539.cos.ap-guangzhou.myqcloud.com/', '')
WHERE `image_url` LIKE 'https://%.myqcloud.com/%';

-- ========== 验证 ==========
-- SELECT id, avatar FROM `mrb_user`.`user` WHERE avatar IS NOT NULL AND avatar != '';
-- SELECT id, image_url FROM `mrb_meeting`.`meeting_room` WHERE image_url IS NOT NULL AND image_url != '';

-- ============================================================
-- 控制台操作提示（人工执行，不在本脚本范围）
-- ============================================================
-- 1. 登录腾讯云 COS 控制台
-- 2. 找到桶 mrb-1310160539
-- 3. 权限管理 → 删除 avatar/ 和 room/ 前缀的公开读策略
-- 4. 桶权限回归「私有读写」
-- 5. 验证：头像/会议室图片/上传预览全链路正常（预签名 URL 可访问）
