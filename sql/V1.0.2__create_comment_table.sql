-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.2
-- 创建时间: 2026-07-11
-- 描述: 添加评论表
-- ============================================

USE aedium;

BEGIN;

-- 创建评论表
CREATE TABLE `comment`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联文章ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
    `content`          TEXT            NULL COMMENT '评论内容(软删除时清空)',
    `root_id`          BIGINT UNSIGNED NULL     DEFAULT NULL COMMENT '根评论ID(一级评论为NULL)',
    `parent_id`        BIGINT UNSIGNED NULL     DEFAULT NULL COMMENT '父评论ID',
    `reply_to_user_id` BIGINT UNSIGNED NULL     DEFAULT NULL COMMENT '回复的用户ID',
    `like_count`       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count`      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '回复数',
    `status`           VARCHAR(16)     NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常，DELETED-已删除，HIDDEN-隐藏',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_article_id` (`article_id`) COMMENT '文章索引',
    INDEX `idx_root_id` (`root_id`) COMMENT '根评论索引',
    INDEX `idx_parent_id` (`parent_id`) COMMENT '父评论索引',
    INDEX `idx_user_id` (`user_id`) COMMENT '用户索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评论表';

CREATE INDEX idx_article_root_status_ct ON comment (article_id, root_id, status, create_time, id);

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
