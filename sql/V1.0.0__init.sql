-- ============================================
-- 数据库初始化脚本
-- 版本: V1.0.0
-- 创建时间: 2026-07-05
-- 描述: Aedium 后端服务数据库初始化
-- ============================================

-- 设置字符集和严格模式
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- 创建数据库（Docker 环境下可选，因为 docker-compose 已配置）
CREATE DATABASE IF NOT EXISTS aedium 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE aedium;

BEGIN;

-- ============================================
-- 用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt/MD5加密）',
  `image` VARCHAR(512) NULL DEFAULT NULL COMMENT '头像URL',
  `user_role` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户角色：1-普通用户，2-管理员',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ============================================
-- 话题表
-- ============================================
DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '话题唯一数字ID',
  `name` VARCHAR(100) NOT NULL COMMENT '话题展示名称',
  `slug` VARCHAR(100) NOT NULL COMMENT 'URL友好别名',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '话题简短介绍',
  `articles_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '话题下文章数量',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-审核中，1-正常，2-封禁',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`),
  INDEX `idx_articles_count` (`articles_count` DESC) COMMENT '热门话题排行索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题表';

-- ============================================
-- 文章表
-- ============================================
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `subtitle` VARCHAR(255) NULL DEFAULT NULL COMMENT '副标题',
  `content` TEXT NOT NULL COMMENT '文章内容',
  `cover_image` VARCHAR(512) NULL DEFAULT NULL COMMENT '文章封面图URL',
  `cover_focus_y` DECIMAL(3,2) NULL DEFAULT 0.5 COMMENT '封面焦点Y坐标(0-1)',
  `author_id` BIGINT UNSIGNED NOT NULL COMMENT '文章作者ID',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_author_id` (`author_id`) COMMENT '作者索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表';

-- ============================================
-- 文章-话题关联表
-- ============================================
DROP TABLE IF EXISTS `article_topic`;
CREATE TABLE `article_topic` (
  `article_id` BIGINT UNSIGNED NOT NULL COMMENT '关联文章ID',
  `topic_id` BIGINT UNSIGNED NOT NULL COMMENT '关联话题ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`article_id`, `topic_id`),
  INDEX `idx_topic_article` (`topic_id`, `article_id`) COMMENT '反向复合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章话题关联表';

-- ============================================
-- 插入初始数据（注意：生产环境应移除或使用环境变量）
-- ============================================
-- 初始管理员用户（密码：123456 的 MD5 值）
INSERT INTO `user` (`username`, `email`, `password`, `image`, `user_role`)
VALUES (
  'Franksoft',
  'frankzhen2025@outlook.com',
  'e10adc3949ba59abbe56e057f20f883e',
  'https://java-web-frank.oss-cn-beijing.aliyuncs.com/2026/06/ec94a9bb-3d76-44a3-8b0b-758a6949d25d.jpg',
  2
) ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 初始化完成
-- ============================================