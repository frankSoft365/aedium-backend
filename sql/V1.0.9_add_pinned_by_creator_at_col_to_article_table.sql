-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.9
-- 创建时间: 2026-10-17
-- 描述: 新增作者置顶时间列到文章表
-- ============================================

USE aedium;
BEGIN;

ALTER TABLE `article`
    ADD COLUMN `pinned_by_creator_at` BIGINT NOT NULL DEFAULT 0 COMMENT '作者置顶毫秒时间戳，0=未置顶';

ALTER TABLE `article`
    ADD INDEX `idx_author_pinned` (`author_id`, `pinned_by_creator_at` DESC, `create_time` DESC);

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
