-- ============================================
-- 数据库迁移脚本
-- 版本: V1.0.3
-- 创建时间: 2026-07-22
-- 描述: 添加通知表
-- ============================================

USE aedium;

BEGIN;

-- 创建通知表
CREATE TABLE `notification`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `recipient_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者用户ID',
    `actor_id`     BIGINT UNSIGNED NOT NULL COMMENT '发起者用户ID',
    `type`         VARCHAR(20)     NOT NULL COMMENT '通知类型：NEW_COMMENT-新评论, NEW_REPLY-新回复, LIKE_ARTICLE-文章点赞, LIKE_COMMENT-评论点赞, NEW_FOLLOWER-新关注',
    `target_type`  VARCHAR(10)     NOT NULL COMMENT '目标类型：ARTICLE-文章, COMMENT-评论, USER-用户',
    `target_id`    BIGINT UNSIGNED NOT NULL COMMENT '目标ID(关联的主实体ID)',
    `params`       JSON            NULL COMMENT '额外参数(存储相关ID等JSON数据)',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知表';

CREATE INDEX idx_recipient_created ON notification (recipient_id, create_time DESC, id DESC);
CREATE INDEX idx_recipient_id ON notification (recipient_id, id);

-- 创建通知阅读状态表
CREATE TABLE `notification_read_state`
(
    `recipient_id`       BIGINT UNSIGNED NOT NULL COMMENT '接收者用户ID',
    `notification_group` VARCHAR(20)     NOT NULL COMMENT '通知分组：reply-回复, like-点赞, follow-关注',
    `last_read_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最后阅读的通知ID',
    `last_read_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    PRIMARY KEY (`recipient_id`, `notification_group`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知阅读状态表';

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================
