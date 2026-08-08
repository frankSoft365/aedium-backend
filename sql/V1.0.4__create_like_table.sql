-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.4
-- 创建时间: 2026-08-07
-- 描述: 创建点赞记录表（软删除方案）
-- ============================================

USE aedium;

BEGIN;

-- 创建点赞记录表
CREATE TABLE `user_like`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '点赞用户ID',
    `target_type` TINYINT UNSIGNED NOT NULL COMMENT '点赞对象类型：1-文章 2-评论',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '点赞对象ID',
    `is_deleted`  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除：0-有效点赞 1-已取消',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`) COMMENT '防重复点赞（软删除标记后可重新点赞）',
    INDEX `idx_target` (`target_type`, `target_id`, `is_deleted`, `create_time`) COMMENT '对象维度查询索引（含有效状态过滤）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户点赞记录表';

-- article 表新增点赞计数
ALTER TABLE `article`
ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数（仅统计有效点赞）' AFTER `author_id`;

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================