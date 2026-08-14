-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.5
-- 创建时间: 2026-08-11
-- 描述: 创建收藏列表及列表-文章关联表
-- ============================================

USE aedium;

BEGIN;

-- 创建收藏列表表
CREATE TABLE `collection_list`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '列表归属用户ID',
    `name`          VARCHAR(100)    NOT NULL COMMENT '列表名称',
    `description`   VARCHAR(500)    NULL DEFAULT NULL COMMENT '列表描述',
    `is_public`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否公开：0-私有 1-公开',
    `is_default`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认列表：0-自定义列表 1-默认列表（每用户仅一个，应用层保证唯一）',
    `article_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '列表内文章数量（冗余计数，收藏/取消时同步维护）',
    `is_delete`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除：0-有效 1-已删除',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user` (`user_id`, `is_delete`, `is_default`) COMMENT '用户列表查询索引（含有效状态与默认标记）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='收藏列表表';

-- 创建收藏列表-文章关联表
CREATE TABLE `collection_list_article`
(
    `list_id`     BIGINT UNSIGNED NOT NULL COMMENT '收藏列表ID',
    `article_id`  BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`list_id`, `article_id`),
    INDEX `idx_article_list` (`article_id`, `list_id`) COMMENT '文章维度反向索引（查询文章被哪些列表收藏）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='收藏列表-文章关联表';

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
