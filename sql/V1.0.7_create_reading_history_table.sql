-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.7
-- 创建时间: 2026-09-29
-- 描述: 创建用户阅读历史表
-- ============================================

USE aedium;

BEGIN;

-- 创建用户阅读历史表
CREATE TABLE `user_reading_history`
(
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `article_id` BIGINT UNSIGNED NOT NULL COMMENT '阅读的文章ID',
    `last_reading_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    PRIMARY KEY (`user_id`, `article_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户阅读历史表';

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
