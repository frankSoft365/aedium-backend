# 通知系统设计文档

## 一、通知表结构

```sql
notification
├── id              主键
├── recipient_id    接收者（通知的归属人）
├── actor_id        发起者（触发通知的人）
├── type            通知类型（NEW_COMMENT / NEW_REPLY / LIKE_ARTICLE / LIKE_COMMENT / NEW_FOLLOWER）
├── target_type     目标类型（ARTICLE / COMMENT / USER）
├── target_id       目标ID（关联的主实体ID）
├── params          额外参数（JSON Map，用于聚合视图补充信息）
└── create_time     创建时间
```

### params 字段设计原则

`params` 是 JSON Map，**仅存储视图渲染必需、但无法从 type + targetId 直接获取的关联信息**。

**判断标准**：如果视图渲染需要某个关联 ID，且该 ID 无法从 `targetId` 推导出来，就放入 `params`。

---

## 二、通知类型与视图

### 2.1 NEW_COMMENT — 评论了我的文章

| 字段 | 值 |
|------|-----|
| type | `NEW_COMMENT` |
| targetType | `COMMENT` |
| targetId | 评论 ID |
| params | `{"articleId": 文章ID}` |

**视图需要**：
- 点赞者信息（actorId → 头像、用户名）
- "评论了我的文章"（type + targetType 推导）
- 评论内容（targetId → 查 comment 表）
- 文章标题（params.articleId → 查 article 表）
- 跳转目标：文章详情页（params.articleId）

### 2.2 NEW_REPLY — 回复了我的（的评论）

| 字段 | 值 |
|------|-----|
| type | `NEW_REPLY` |
| targetType | `COMMENT` |
| targetId | 回复评论 ID |
| params | `{"articleId": 文章ID, "rootId": 根评论ID, "parentId": 父评论ID, "parentReplyToUserId": 父评论回复对象ID}` |

**视图需要**：
- 回复者信息（actorId → 头像、用户名）
- "回复了我"（type + targetType 推导）
- 回复内容（targetId → 查 comment 表）
- 父评论内容（params.parentId → 查 comment 表）
- 根评论内容（params.rootId → 查 comment 表）
- 文章标题（params.articleId → 查 article 表）
- 跳转目标：文章详情页（params.articleId）

### 2.3 LIKE_ARTICLE — 赞了我的文章

| 字段 | 值 |
|------|-----|
| type | `LIKE_ARTICLE` |
| targetType | `ARTICLE` |
| targetId | 文章 ID |
| params | `null` |

**视图需要**：
- 点赞者信息（actorId → 头像、用户名）
- "赞了我的文章"（type + targetType 推导）
- 文章标题（targetId → 查 article 表，无需 params）
- 跳转目标：文章详情页（targetId 即文章 ID）

**params 为 null 的原因**：targetId 就是文章 ID，视图渲染和跳转所需的信息全部可从 targetId 获取。

### 2.4 LIKE_COMMENT — 赞了我的评论

| 字段 | 值 |
|------|-----|
| type | `LIKE_COMMENT` |
| targetType | `COMMENT` |
| targetId | 评论 ID |
| params | `{"articleId": 文章ID}` |

**视图需要**：
- 点赞者信息（actorId → 头像、用户名）
- "赞了我的评论"（type + targetType 推导）
- 评论内容（targetId → 查 comment 表）
- 跳转目标：文章详情页（**需要 params.articleId**）

**params 需要 articleId 的原因**：targetId 是评论 ID，不是文章 ID。前端跳转需要文章 ID，评论本身不直接暴露文章 ID 到视图，所以需要 params 补充。

### 2.5 NEW_FOLLOWER — 关注了我

| 字段 | 值 |
|------|-----|
| type | `NEW_FOLLOWER` |
| targetType | `USER` |
| targetId | 关注者用户 ID |
| params | `null` |

**视图需要**：
- 关注者信息（actorId / targetId → 头像、用户名）
- "关注了我"（type + targetType 推导）
- 跳转目标：用户主页（targetId 即用户 ID）

---

## 三、params 设计总结

| 通知类型 | params | 原因 |
|---------|--------|------|
| NEW_COMMENT | `{"articleId": ...}` | targetId 是评论ID，跳转需要文章ID |
| NEW_REPLY | `{"articleId": ..., "rootId": ..., "parentId": ..., "parentReplyToUserId": ...}` | 需要文章ID、评论层级关系 |
| LIKE_ARTICLE | `null` | targetId 即文章ID，无需补充 |
| LIKE_COMMENT | `{"articleId": ...}` | targetId 是评论ID，跳转需要文章ID |
| NEW_FOLLOWER | `null` | targetId 即用户ID，无需补充 |

**规律**：当 `targetId` 指向的不是最终跳转目标时，需要用 `params` 补充跳转目标 ID。

---

## 四、通知分组

通知按业务场景分为三组，每组独立维护已读水位线：

| 分组 | 包含类型 | 说明 |
|------|---------|------|
| `reply` | NEW_COMMENT, NEW_REPLY | 评论与回复 |
| `like` | LIKE_ARTICLE, LIKE_COMMENT | 点赞 |
| `follow` | NEW_FOLLOWER | 关注 |

---

## 五、通知去重策略

对于点赞通知，采用**幂等创建**策略：

- 创建通知前，先查询 `(recipientId, actorId, type, targetType, targetId)` 是否已存在
- 若存在，跳过创建（用户取消后重新点赞不会产生重复通知）
- 若不存在，创建新通知

```java
long exists = notificationService.count(new LambdaQueryWrapper<Notification>()
        .eq(Notification::getRecipientId, recipientId)
        .eq(Notification::getActorId, actorId)
        .eq(Notification::getType, notificationType.getValue())
        .eq(Notification::getTargetType, notificationTargetType.getValue())
        .eq(Notification::getTargetId, targetId));

if (exists == 0) {
    notificationService.createNotification(recipientId, actorId, type, targetType, targetId, params);
}
```

---

## 六、通知创建规则

| 规则 | 说明 |
|------|------|
| 不给自己发通知 | `recipientId == actorId` 时跳过 |
| 点赞通知去重 | 同一用户对同一对象的点赞通知只创建一次 |
| 取消点赞不删通知 | 取消点赞时，通知记录保留（只是计数 -1） |

---

## 七、实时推送

通知创建后通过 WebSocket + STOMP 异步推送给接收者，前端收到后可直接渲染新通知条目并更新未读角标。

### 7.1 推送通道

| 项 | 值 |
|----|-----|
| 协议 | WebSocket + STOMP |
| 握手端点 | `/ws`（握手时从 cookie 校验 token，校验通过后将 `userId` 作为 Principal 名） |
| 用户消息前缀 | `/user` |
| Broker 前缀 | `/queue` |
| **前端订阅地址** | `/user/queue/notifications` |

配置见 [WebSocketConfig.java](file:///Users/frank/javaProject/aedium-backend/aedium-backend/src/main/java/com/microsoft/aediumbackend/config/WebSocketConfig.java)。

### 7.2 推送时机（关键）

通知持久化与计数更新在**同一事务**内完成；推送**必须在事务提交后**异步触发，否则推送线程查不到未提交的通知数据，导致 VO 聚合失败。

- 点赞场景：`UserLikeService.handleAction` 是事务入口，通过 `TransactionSynchronizationManager.registerSynchronization` 注册 `afterCommit` 回调，事务提交后调用 `LikePushService.pushLikeNotification`
- 评论场景：`CommentPersistService.persistComment` 是事务方法，事务提交后 `CommentServiceImpl.addComment` 调用 `CommentPostProcessService.handlePostCommentTasks`（`@Async`）

### 7.3 推送消息体

```json
{
  "recipientId": 123,
  "type": "like",
  "unreadCount": 2,
  "notificationVO": { /* LikeNotificationVO 或 ReplyNotificationVO，follow 类型为 null */ }
}
```

| 字段 | 说明 |
|------|------|
| recipientId | 接收者用户 ID |
| type | 通知分组（`reply` / `like` / `follow`） |
| unreadCount | 该分组当前未读数（用于更新角标） |
| notificationVO | 完整通知视图，前端可直接渲染新通知条目 |

DTO 定义见 [NotificationPushRequest.java](file:///Users/frank/javaProject/aedium-backend/aedium-backend/src/main/java/com/microsoft/aediumbackend/model/dto/notificationPush/request/NotificationPushRequest.java)。

### 7.4 触发点（各业务方自行触发）

| 通知分组 | 触发位置 | 推送服务 | 状态 |
|---------|---------|---------|------|
| reply | `CommentServiceImpl.addComment` | `CommentPostProcessService.handlePostCommentTasks` | 已接入 |
| like | `UserLikeService.handleAction` | `LikePushService.pushLikeNotification` | 已接入 |
| follow | — | — | 待接入（本次仅推 unreadCount，notificationVO 留空） |

### 7.5 异步线程池

推送使用专用线程池 `notificationPushExecutor`（core=2, max=8, queue=200），与评论业务线程池 `commentTaskExecutor` 隔离，避免推送耗时影响业务响应。拒绝策略为 `CallerRunsPolicy`（队列满时由调用线程执行，不丢消息）。配置见 [AsyncConfig.java](file:///Users/frank/javaProject/aedium-backend/aedium-backend/src/main/java/com/microsoft/aediumbackend/config/AsyncConfig.java)。

### 7.6 异常处理

推送方法是 `@Async void`，异常**不会传播给调用方**，必须在推送方法内部 try-catch，仅记录日志，不影响主业务事务。
