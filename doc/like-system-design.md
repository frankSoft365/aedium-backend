# 点赞系统设计文档

## 1. 概述

### 1.1 功能范围

- 用户对**文章**点赞 / 取消点赞
- 用户对**评论**点赞 / 取消点赞
- 支持**重新点赞**（取消后再次点赞，复用旧记录）
- 点赞时自动创建**通知**给被点赞对象的作者
- 支持**通知去重**（同一用户重新点赞不重复推送通知）

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **软删除** | 取消点赞使用 `is_deleted=1` 标记，保留历史记录 |
| **幂等性** | 重复点赞 / 取消不报错，通过唯一索引 + 业务判断保证 |
| **事务一致性** | 点赞记录 + 计数更新 + 通知创建在同一事务中 |
| **异步推送** | 通知推送走 WebSocket 异步，不阻塞主流程 |

---

## 2. 表结构

### 2.1 点赞记录表

```sql
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
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`) COMMENT '防重复点赞',
    INDEX `idx_target` (`target_type`, `target_id`, `is_deleted`, `create_time`) COMMENT '对象维度查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT ='用户点赞记录表';
```

### 2.2 文章表新增字段

```sql
ALTER TABLE `article`
ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `author_id`;
```

### 2.3 字段枚举

**target_type 枚举**

| 值 | 含义 |
|----|------|
| 1 | 文章（ARTICLE） |
| 2 | 评论（COMMENT） |

### 2.4 与通知系统的关联

通知类型已存在（[NotificationType.java](../src/main/java/com/microsoft/aediumbackend/model/enums/NotificationType.java)）：

| 枚举值 | 用途 |
|--------|------|
| `LIKE_ARTICLE` | 文章点赞通知 |
| `LIKE_COMMENT` | 评论点赞通知 |

通知目标类型已存在（[NotificationTargetType.java](../src/main/java/com/microsoft/aediumbackend/model/enums/NotificationTargetType.java)）：

| 枚举值 | 用途 |
|--------|------|
| `ARTICLE` | 文章 |
| `COMMENT` | 评论 |

---

## 3. API 设计

### 3.1 点赞 / 取消点赞

**URL**: `POST /like/action`

**请求体**:

```json
{
    "targetType": 1,
    "targetId": 100,
    "action": 1
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetType | Integer | ✅ | 1-文章 2-评论 |
| targetId | Long | ✅ | 点赞对象 ID |
| action | Integer | ✅ | 1-点赞 2-取消点赞 |

**响应体**:

```json
{
    "code": 0,
    "data": {
        "likeCount": 15,
        "isLiked": true
    }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| likeCount | Integer | 对象当前最新点赞数 |
| isLiked | Boolean | 操作后的点赞状态 |

### 3.2 查询点赞状态（批量）

**说明**：文章/评论表中已有 `like_count` 冗余字段，点赞数直接随列表接口返回。仅"当前用户是否已点赞"需要单独查询。

**URL**: `POST /like/batch-status`

**请求体**:

```json
{
    "targetType": 1,
    "targetIds": [100, 101, 102]
}
```

**响应体**:

```json
{
    "code": 0,
    "data": {
        "100": true,
        "101": false,
        "102": true
    }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | Map\<String, Boolean\> | key=targetId，value=是否已点赞 |

**使用场景**：文章列表/评论列表页面，前端批量传入 ID，获取每个对象的点赞状态。

---

## 4. 业务流程

### 4.1 文章点赞流程

```
用户A点赞文章X
│
├── 1. 查询 user_like 表是否存在 (user_id=A, target_type=1, target_id=X)
│   │
│   ├── 不存在 → INSERT 新记录 (is_deleted=0)
│   │            → UPDATE article SET like_count = like_count + 1
│   │            → 查询文章作者 authorId
│   │            → 查询是否已存在通知 (recipient_id=authorId, actor_id=A, type=LIKE_ARTICLE, target_id=X)
│   │            │
│   │            ├── 不存在 → 创建通知 Notification(LIKE_ARTICLE)
│   │            │           → 异步推送通知给作者
│   │            │
│   │            └── 已存在 → 跳过通知创建（去重）
│   │
│   └── 存在且 is_deleted=1 → UPDATE SET is_deleted=0
│                             → UPDATE article SET like_count = like_count + 1
│                             → 查询通知是否已存在 → 同上去重逻辑
│
│   存在且 is_deleted=0 → 直接返回当前状态（幂等）
│
└── 返回 { likeCount: N, isLiked: true }
```

### 4.2 文章取消点赞流程

```
用户A取消点赞文章X
│
├── 1. 查询 user_like 表
│   │
│   ├── 存在且 is_deleted=0 → UPDATE SET is_deleted=1
│   │   → UPDATE article SET like_count = GREATEST(like_count - 1, 0)
│   │
│   └── 不存在或 is_deleted=1 → 直接返回（幂等）
│
└── 返回 { likeCount: N, isLiked: false }
```

### 4.3 评论点赞流程

与文章点赞流程完全一致，差异点：

| 维度 | 文章点赞 | 评论点赞 |
|------|---------|---------|
| target_type | 1 | 2 |
| 通知类型 | LIKE_ARTICLE | LIKE_COMMENT |
| 通知 target_type | ARTICLE | COMMENT |
| 被通知者 | 文章作者 | 评论作者 |
| 计数更新字段 | article.like_count | comment.like_count（已存在） |

### 4.4 通知去重逻辑

**核心规则**：同一用户对同一对象的点赞，只创建一次通知。

```sql
-- 查询是否已存在点赞通知（用于去重）
SELECT id FROM notification
WHERE recipient_id = #{recipientId}
  AND actor_id = #{actorId}
  AND type = #{notificationType}
  AND target_type = #{targetType}
  AND target_id = #{targetId}
LIMIT 1;
```

| 场景 | 通知处理 |
|------|---------|
| 首次点赞 | 创建通知 |
| 取消点赞 | 不删除通知（保留历史） |
| 重新点赞 | 检查已有通知 → 存在则跳过，不存在则创建 |

---

## 5. 事务设计

### 5.1 主事务（同步）

点赞操作涉及的数据库操作在**同一事务**中：

```java
@Transactional(rollbackFor = Exception.class)
public LikeResult like(Long userId, Integer targetType, Long targetId) {
    // 1. 查询/更新 user_like 记录
    // 2. 更新冗余计数（article.like_count 或 comment.like_count）
    // 3. 查询被通知者信息
    // 4. 检查通知是否存在，不存在则创建
    // 以上操作在同一事务中
}
```

### 5.2 异步推送（独立）

通知推送走 `@Async`，不阻塞主事务：

```java
@Async("likeTaskExecutor")
public void pushNotification(Long recipientId, NotificationPushRequest request) {
    notificationPushService.pushUnreadCount(request);
}
```

### 5.3 事务边界

| 操作 | 事务 | 说明 |
|------|------|------|
| 点赞记录写入 | 主事务 | 与计数更新绑定 |
| 计数 +1 / -1 | 主事务 | 保证数据一致性 |
| 通知创建 | 主事务 | 与点赞记录绑定，失败则回滚 |
| 通知推送 | 独立异步 | 推送失败不影响主流程 |

---

## 6. 类结构设计

### 6.1 新增类清单

```
model/
├── entity/
│   └── UserLike.java              # 点赞记录实体
├── dto/
│   ├── like/
│   │   ├── LikeActionRequest.java      # 点赞/取消请求
│   │   ├── LikeActionResult.java       # 操作结果
│   │   ├── LikeBatchStatusRequest.java # 批量状态查询
│   │   └── LikeBatchStatusResult.java  # 批量状态结果
│   └── vo/
│       └── ArticleListItemVO.java      # 扩展 likeCount / isLiked 字段
mapper/
└── UserLikeMapper.java            # 点赞记录 Mapper
service/
├── UserLikeService.java           # 点赞服务接口
└── impl/
    └── UserLikeServiceImpl.java    # 点赞服务实现
controller/
└── LikeController.java            # 点赞 API 控制器
```

### 6.2 核心依赖

| 依赖 | 用途 |
|------|------|
| UserLikeMapper | 点赞记录 CRUD |
| NotificationService | 创建点赞通知 |
| NotificationPushService | 异步推送通知 |
| ArticleMapper | 更新文章点赞数 |
| CommentMapper | 更新评论点赞数 |

### 6.3 枚举定义

```java
// TargetType
@Getter
@AllArgsConstructor
public enum LikeTargetType {
    ARTICLE(1, "ARTICLE"),
    COMMENT(2, "COMMENT");
    
    private final int code;
    private final String label;
}
```

---

## 7. SQL 关键语句

### 7.1 查询是否存在有效点赞

```sql
SELECT id, is_deleted
FROM user_like
WHERE user_id = #{userId}
  AND target_type = #{targetType}
  AND target_id = #{targetId}
LIMIT 1;
```

### 7.2 重新点赞（软删除恢复）

```sql
UPDATE user_like
SET is_deleted = 0, update_time = CURRENT_TIMESTAMP
WHERE user_id = #{userId}
  AND target_type = #{targetType}
  AND target_id = #{targetId}
  AND is_deleted = 1;
```

### 7.3 取消点赞

```sql
UPDATE user_like
SET is_deleted = 1, update_time = CURRENT_TIMESTAMP
WHERE user_id = #{userId}
  AND target_type = #{targetType}
  AND target_id = #{targetId}
  AND is_deleted = 0;
```

### 7.4 文章点赞数 +1 / -1

```sql
-- +1
UPDATE article SET like_count = like_count + 1 WHERE id = #{articleId} AND is_delete = 0;

-- -1（防负数）
UPDATE article SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{articleId} AND is_delete = 0;
```

### 7.5 评论点赞数 +1 / -1

```sql
-- +1
UPDATE comment SET like_count = like_count + 1 WHERE id = #{commentId};

-- -1（防负数）
UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{commentId};
```

### 7.6 批量查询点赞状态

```sql
SELECT target_id
FROM user_like
WHERE user_id = #{userId}
  AND target_type = #{targetType}
  AND is_deleted = 0
  AND target_id IN (100, 101, 102);
```

### 7.6 检查通知是否存在（去重）

```sql
SELECT id
FROM notification
WHERE recipient_id = #{recipientId}
  AND actor_id = #{actorId}
  AND type = #{notificationType}
  AND target_type = #{targetType}
  AND target_id = #{targetId}
LIMIT 1;
```

---

## 8. 前端集成

### 8.1 请求参数

| 场景 | targetType | action |
|------|-----------|--------|
| 点赞文章 | 1 | 1 |
| 取消文章点赞 | 1 | 2 |
| 点赞评论 | 2 | 1 |
| 取消评论点赞 | 2 | 2 |

### 8.2 文章列表展示

文章列表接口需扩展返回字段：

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| likeCount | Integer | 文章点赞数 |
| isLiked | Boolean | 当前用户是否已点赞 |

### 8.3 评论列表展示

评论列表已返回 `likeCount` 字段（Comment 实体已有），需扩展：

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| isLiked | Boolean | 当前用户是否已点赞 |

---

## 9. 风险与注意事项

### 9.1 并发场景

| 场景 | 风险 | 解决方案 |
|------|------|---------|
| 高并发点赞 | 计数不准确 | `UPDATE ... SET like_count = like_count + 1` 原子操作 |
| 重复提交 | 数据异常 | 唯一索引 `uk_user_target` 兜底 |
| 快速连点 | 计数错乱 | 前端防抖 + 后端幂等 |

### 9.2 数据一致性

| 场景 | 风险 | 解决方案 |
|------|------|---------|
| 事务部分成功 | 记录与计数不一致 | `@Transactional` 回滚 |
| 通知创建失败 | 数据不一致 | 通知在主事务中创建，失败则回滚整个操作 |
| 推送失败 | 用户收不到通知 | 推送走异步，失败不影响主流程；可考虑失败重试 |

### 9.3 后续扩展

| 扩展方向 | 说明 |
|---------|------|
| 话题点赞 | target_type 新增 3-TOPIC |
| 批量点赞 | 后端已支持批量状态/计数查询 |
| 排行榜 | 基于 `like_count` 字段排序即可 |
| 点赞缓存 | 高并发场景引入 Redis 缓存计数，异步落库 |

---

## 10. 实施步骤

| 步骤 | 内容 | 优先级 |
|------|------|--------|
| 1 | 执行 SQL 迁移脚本 V1.0.4 | P0 |
| 2 | 创建 UserLike 实体类 + Mapper | P0 |
| 3 | 创建点赞 DTO（请求/响应） | P0 |
| 4 | 实现 UserLikeService（点赞/取消/重新点赞） | P0 |
| 5 | 实现 LikeController API | P0 |
| 6 | 集成通知系统（创建 + 去重检查） | P0 |
| 7 | 扩展文章列表/详情接口返回点赞状态 | P1 |
| 8 | 扩展评论列表接口返回点赞状态 | P1 |
| 9 | 配置点赞异步推送线程池 | P1 |
| 10 | 编写单元测试 + 接口测试 | P1 |

---

## 附录：文件路径索引

| 文件 | 路径 |
|------|------|
| 设计文档 | `doc/like-system-design.md` |
| SQL 迁移 | `sql/V1.0.4__create_like_table.sql` |
| 通知类型枚举 | `src/.../model/enums/NotificationType.java` |
| 通知目标类型枚举 | `src/.../model/enums/NotificationTargetType.java` |
| 通知服务接口 | `src/.../service/NotificationService.java` |
| 通知服务实现 | `src/.../service/impl/NotificationServiceImpl.java` |
| 通知推送服务 | `src/.../service/impl/notification/NotificationPushService.java` |
| 异步配置 | `src/.../config/AsyncConfig.java` |
| 评论实体 | `src/.../model/entity/Comment.java`（已有 like_count） |
| 文章实体 | `src/.../model/entity/Article.java`（待新增 like_count） |