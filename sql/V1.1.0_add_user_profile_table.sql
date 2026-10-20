-- ============================================
-- 数据库迁移脚本
-- 版本: V1.1.0
-- 创建时间: 2026-10-19
-- 描述: 新增用户主页信息表
-- ============================================

USE aedium;
BEGIN;

CREATE TABLE `user_profile`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID（关联user表id）',
    `bio`         VARCHAR(255)    NULL     DEFAULT NULL COMMENT '个人简介（短签名）',
    `about`       TEXT            NULL     DEFAULT NULL COMMENT '关于页面详细介绍（支持富文本/长文）',
    `is_delete`   TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户扩展资料表';

INSERT INTO `user_profile` (`user_id`, `create_time`, `update_time`)
SELECT
    u.`id` AS `user_id`,
    u.`create_time`,
    CURRENT_TIMESTAMP
FROM `user` u
         LEFT JOIN `user_profile` p ON u.`id` = p.`user_id`
WHERE p.`user_id` IS NULL;

COMMIT;

-- ============================================
-- 迁移完成
-- ============================================