-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.6
-- 创建时间: 2026-08-24
-- 描述: 创建用户关注关系表并添加关注冗余计数
-- ============================================

USE aedium;

BEGIN;

-- 创建用户关注关系表
CREATE TABLE `user_follow`
(
    `follower_id` BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
    `followed_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`follower_id`, `followed_id`),
    INDEX `idx_followed_time`
        (`followed_id`, `create_time` DESC, `follower_id` DESC) COMMENT '粉丝列表索引',
    INDEX `idx_follower_time`
        (`follower_id`, `create_time` DESC, `followed_id` DESC) COMMENT '关注列表索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户关注关系表';

-- user 表新增粉丝数和关注数
ALTER TABLE `user`
    ADD COLUMN `follower_count` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '粉丝数量（冗余计数）' AFTER `user_role`,
    ADD COLUMN `following_count` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '关注用户数量（冗余计数）' AFTER `follower_count`;

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
