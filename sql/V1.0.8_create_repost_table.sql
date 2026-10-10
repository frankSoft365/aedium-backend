-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.8
-- 创建时间: 2026-10-06
-- 描述: 创建用户转发表
-- ============================================

USE aedium;

BEGIN;

-- 创建用户转发表
CREATE TABLE `user_repost`
(
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `article_id` BIGINT UNSIGNED NOT NULL COMMENT '转发的文章ID',
    `note` VARCHAR(300) NULL COMMENT '转发笔记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '转发时间',
    PRIMARY KEY (`user_id`, `article_id`),
    INDEX `idx_article_id` (`article_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户转发表';

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
