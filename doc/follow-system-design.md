# 关注系统功能设计文档

## 1. 需求范围

### 1.1 功能目标

- 用户可以关注其他用户，也可以取消关注。
- 当前用户自己的信息区域不展示关注按钮，后端同时禁止关注自己。
- 个人主页展示该用户的粉丝数。
- 个人主页“关于”区域展示该用户的关注数。
- 点击粉丝数可查看粉丝列表。
- 点击关注数可查看该用户关注的用户列表。
- 所有出现关注按钮的位置都能获得当前用户对目标用户的关注状态。
- 单个用户最多关注 5000 个用户，粉丝数量不设业务上限。
- 关注和取消关注成功后由前端展示 toast。

### 1.2 按钮出现位置

| 场景 | 展示条件 | 状态来源 |
|------|----------|----------|
| 文章列表项下拉框中的作者信息 | 作者不是当前用户 | `POST /follow/batch-status` |
| 文章详情页作者名字旁 | 作者不是当前用户 | `POST /follow/batch-status`，单个 ID 也使用该接口 |
| 用户个人主页 | 页面用户不是当前用户 | 使用批量状态接口查询主页用户的关注状态 |
| 粉丝/关注列表 | 列表项用户不是当前用户 | 收集列表用户 ID 后调用批量状态接口 |

### 1.3 不在本期范围

- 私密账号与关注审批。
- 拉黑、屏蔽及黑名单联动。
- 好友或互相关注关系。
- 粉丝移除。
- 关注推荐与关注动态流。

---

## 2. 名词与核心规则

| 名词 | 含义 |
|------|------|
| 关注者 `follower` | 发起关注的用户 |
| 被关注者 `followed` | 接收关注的用户 |
| 关注数 `followingCount` | 用户当前关注了多少人 |
| 粉丝数 `followerCount` | 用户当前被多少人关注 |

关注关系是有方向的：

```text
follower_id  ->  followed_id
关注者           被关注者
```

核心规则：

1. 使用 `(follower_id, followed_id)` 联合主键保证关注关系全局唯一。
2. 取消关注物理删除关系记录，重新关注时插入新记录。
3. 重复关注和重复取消均按成功处理，不重复修改计数。
4. 关注关系、两个用户的冗余计数、关注通知必须在同一事务内写入。
5. 关注成功后，关注者的 `following_count + 1`，被关注者的 `follower_count + 1`。
6. 取消成功后两个计数分别减 1，并使用 `GREATEST(count - 1, 0)` 防止负数。
7. `following_count` 达到 5000 后，不允许新增其他关注关系。
8. 所有返回前端的 ID 由全局 Jackson 配置序列化为字符串，避免 JavaScript 精度丢失。

---

## 3. 数据库设计

迁移脚本：`sql/V1.0.6__create_follow_table.sql`。

### 3.1 用户关注表

```sql
CREATE TABLE `user_follow`
(
    `follower_id`  BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
    `followed_id`  BIGINT UNSIGNED NOT NULL COMMENT '被关注者用户ID',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`follower_id`, `followed_id`),
    INDEX `idx_followed_time`
        (`followed_id`, `create_time` DESC, `follower_id` DESC) COMMENT '粉丝列表索引',
    INDEX `idx_follower_time`
        (`follower_id`, `create_time` DESC, `followed_id` DESC) COMMENT '关注列表索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户关注关系表';
```

说明：

- 不建立数据库外键，与现有表结构保持一致，关联完整性由服务层保证。
- 表中只保存当前有效的关注关系，取消关注时直接删除记录。
- `create_time` 表示本次关注关系的建立时间，重新关注会生成新记录和新的关注时间。
- 联合主键同时承担关系标识、幂等和并发兜底作用，不额外设置代理 ID。
- 两个列表索引分别覆盖“谁关注了该用户”和“该用户关注了谁”的游标查询。

### 3.2 用户表冗余计数

```sql
ALTER TABLE `user`
    ADD COLUMN `follower_count` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '粉丝数量（冗余计数）' AFTER `user_role`,
    ADD COLUMN `following_count` INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '关注用户数量（冗余计数）' AFTER `follower_count`;
```

计数定义：

```text
user.follower_count =
    user_follow 中 followed_id = user.id 的记录数

user.following_count =
    user_follow 中 follower_id = user.id 的记录数
```

冗余计数只用于页面展示和 5000 上限判断，关注关系表仍是最终事实来源。

---

## 4. API 设计

所有关注接口均要求登录。

### 4.1 关注/取消关注

**URL**：`POST /follow/action`

**请求体**：

```json
{
  "targetUserId": "10001",
  "action": 1
}
```

| 参数 | Java 类型 | 必填 | 说明 |
|------|-----------|------|------|
| targetUserId | Long | 是 | 目标用户 ID |
| action | Integer | 是 | `1`-关注，`2`-取消关注 |

**成功响应**：

```json
{
  "code": 0,
  "data": null,
  "message": "success",
  "description": null
}
```

接口返回 `void`，前端在请求成功后更新本地按钮状态；需要刷新数量时重新获取用户信息。

### 4.2 批量查询关注状态

**URL**：`POST /follow/batch-status`

**请求体**：

```json
{
  "targetUserIds": ["10001", "10002", "10003"]
}
```

约束：

- `targetUserIds` 不能为空。
- 单次最多查询 100 个用户。
- 服务端先去重再查询。

**响应体**：

```json
{
  "code": 0,
  "data": {
    "followingMap": {
      "10001": true,
      "10002": false,
      "10003": true
    }
  },
  "message": "success",
  "description": null
}
```

结果必须包含请求中的每个合法用户 ID；不存在关注关系时返回 `false`。该接口允许仅传一个 ID，不再额外设计单用户状态接口。

### 4.3 查询粉丝列表

**URL**：`GET /follow/followers`

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 被查看用户 ID |
| lastCreatedAt | LocalDateTime | 否 | 上一页最后一条关注时间 |
| lastId | Long | 否 | 上一页最后一条用户 ID；粉丝列表为关注者 ID |
| size | Integer | 否 | 默认 12，范围 1-50 |

**响应体**：

```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "username": "example_user",
        "id": "20001",
        "image": "https://example.com/avatar.jpg",
        "email": "example@example.com",
        "userRole": 0,
        "createTime": "2026-08-01T10:00:00",
        "followerCount": 120,
        "followingCount": 35
      }
    ],
    "hasMore": false,
    "nextCursorCreatedAt": "2026-08-24T12:30:00",
    "nextCursorId": "20001"
  },
  "message": "success",
  "description": null
}
```

列表含义：查询 `followed_id = userId` 的有效关系，返回关注者信息。

### 4.4 查询关注列表

**URL**：`GET /follow/following`

参数和响应结构与粉丝列表一致；关注列表中的 `lastId`、`nextCursorId` 表示被关注者 ID。

列表含义：查询 `follower_id = userId` 的有效关系，返回被关注者信息。

### 4.5 用户信息接口扩展

现有接口：

- `GET /user/current`
- `GET /user/some?userId={userId}`

在 `UserVO` 中增加：

| 字段 | 类型 | 说明 |
|------|------|------|
| followerCount | Integer | 粉丝数 |
| followingCount | Integer | 关注数 |

示例：

```json
{
  "id": "10001",
  "username": "example_user",
  "image": "https://example.com/avatar.jpg",
  "followerCount": 120,
  "followingCount": 35
}
```

个人主页和“关于”区域直接使用同一份用户信息，不额外发送计数查询请求。

---

## 5. DTO 与类结构

### 5.1 新增类

```text
model/
├── entity/
│   └── UserFollow.java
├── dto/
│   └── follow/
│       ├── FollowActionRequest.java
│       ├── FollowBatchStatusRequest.java
│       └── FollowBatchStatusResult.java
mapper/
└── UserFollowMapper.java
service/
├── UserFollowService.java
└── impl/
    └── follow/
        └── FollowPushService.java
controller/
└── FollowController.java
```

`UserFollowService` 使用单一服务类，不再拆分接口与实现类。常规关系查询和更新优先使用 MyBatis-Plus `BaseMapper`、`LambdaQueryWrapper` 和 `LambdaUpdateWrapper`。由于关系表使用联合主键，查询和删除均通过 `follower_id + followed_id` 条件执行，不使用 `selectById` 或 `deleteById`。

### 5.2 DTO 字段

```java
public class FollowActionRequest {
    private Long targetUserId;
    private Integer action;
}

public class FollowBatchStatusRequest {
    private List<Long> targetUserIds;
}

public class FollowBatchStatusResult {
    private Map<String, Boolean> followingMap;
}
```

请求 DTO 使用 Jakarta Validation 注解并由 Controller 通过 `@Valid` 校验。Controller 只负责接收参数和调用 Service，业务校验全部放在 Service。

---

## 6. 业务流程

### 6.1 关注

```text
当前用户 A 关注用户 B
│
├── 校验登录态、action 和 targetUserId
├── 校验 A != B
├── 按用户 ID 升序锁定 A、B 两行用户数据
├── 校验 B 存在且未删除
├── 查询关系 (follower_id=A, followed_id=B)
│
├── 已存在关注关系
│   └── 幂等成功，不修改计数、不创建通知
│
├── A.following_count >= 5000
│   └── 返回“关注数量不能超过5000”
│
├── 关系不存在
│   └── 插入 user_follow
│
├── A.following_count + 1
├── B.follower_count + 1
├── 创建 NEW_FOLLOWER 通知（不存在时）
└── 事务提交后异步推送通知
```

先判断是否已经关注，再判断 5000 上限。这样用户达到上限后重复提交已有关注，仍可幂等成功。

### 6.2 取消关注

```text
当前用户 A 取消关注用户 B
│
├── 校验登录态、action 和 targetUserId
├── 按用户 ID 升序锁定 A、B 两行用户数据
├── 查询关系 (follower_id=A, followed_id=B)
│
├── 关系不存在
│   └── 幂等成功，不修改计数
│
└── 关系存在
    ├── 物理删除 user_follow 记录
    ├── A.following_count = GREATEST(following_count - 1, 0)
    ├── B.follower_count = GREATEST(follower_count - 1, 0)
    └── 不删除历史通知
```

### 6.3 为什么锁定两个用户

同一事务会修改关注者和被关注者两行用户数据。按用户 ID 升序一次性锁定两行，可以：

- 串行化同一用户的并发关注请求，严格保证 5000 上限。
- 避免重复修改冗余计数。
- 避免 A 关注 B 与 B 关注 A 时因锁顺序相反造成死锁。

可使用 MyBatis-Plus 查询包装器追加 `FOR UPDATE`，无需新增 XML 查询：

```java
new LambdaQueryWrapper<User>()
        .in(User::getId, List.of(currentUserId, targetUserId))
        .orderByAsc(User::getId)
        .last("FOR UPDATE");
```

该查询必须在 `@Transactional(rollbackFor = Exception.class)` 方法内执行。

---

## 7. 列表与状态查询

### 7.1 游标分页

粉丝列表按最新关注时间倒序：

```sql
SELECT follower_id, create_time
FROM user_follow
WHERE followed_id = #{userId}
  AND (
      #{lastCreatedAt} IS NULL
      OR create_time < #{lastCreatedAt}
      OR (create_time = #{lastCreatedAt} AND follower_id < #{lastId})
  )
ORDER BY create_time DESC, follower_id DESC
LIMIT #{sizePlusOne};
```

关注列表将筛选字段改为 `follower_id`，返回 `followed_id`，并按 `create_time DESC, followed_id DESC` 排序。

查询 `size + 1` 条判断 `hasMore`，避免总数统计。关系分页完成后批量查询用户信息和当前登录用户对列表用户的关注状态，禁止逐条查询产生 N+1。

### 7.2 批量状态

```sql
SELECT followed_id
FROM user_follow
WHERE follower_id = #{currentUserId}
  AND followed_id IN (...);
```

前端请求约定：

| 页面 | 查询策略 |
|------|----------|
| 文章列表 | 收集当前页全部作者 ID，去重后批量查询一次 |
| 文章详情 | 使用作者 ID 调用一次批量接口 |
| 用户主页 | 使用批量状态接口查询主页用户的关注状态 |
| 粉丝/关注列表 | 收集当前页用户 ID，调用一次批量状态接口 |

---

## 8. 通知设计

项目通知系统已包含 `NEW_FOLLOWER` 和 `USER`，无需新增枚举。

| 字段 | 值 |
|------|-----|
| recipientId | 被关注者 ID |
| actorId | 关注者 ID |
| type | `NEW_FOLLOWER` |
| targetType | `USER` |
| targetId | 关注者 ID |
| params | `null` |

规则：

1. 仅首次建立该关注关系时创建通知。
2. 取消关注不删除历史通知。
3. 取消后重新关注不重复创建通知，避免通过反复关注刷通知。
4. 通知记录与关注关系、计数在同一事务中创建。
5. WebSocket 推送在事务提交后由 `FollowPushService` 异步执行。
6. 推送分组为 `follow`；根据现有通知设计，`notificationVO` 暂时为 `null`，推送最新未读数。
7. 异步推送失败只记录日志，不回滚已提交的关注关系。

通知去重条件：

```sql
SELECT id
FROM notification
WHERE recipient_id = #{followedId}
  AND actor_id = #{followerId}
  AND type = 'NEW_FOLLOWER'
  AND target_type = 'USER'
  AND target_id = #{followerId}
LIMIT 1;
```

---

## 9. 前端交互

### 9.1 关注

1. 用户点击“关注”。
2. 按钮进入 loading 并禁止重复点击。
3. 请求 `action=1`。
4. 成功后按钮变为“已关注”。
5. 展开包含“取消关注”的下拉框。
6. 展示关注成功 toast。
7. 失败时恢复“关注”状态并展示错误 toast。

### 9.2 取消关注

1. 用户点击“已关注”，只展开下拉框，不发送请求。
2. 用户点击“取消关注”。
3. 菜单项进入 loading 并禁止重复点击。
4. 请求 `action=2`。
5. 成功后关闭下拉框，按钮变为“关注”。
6. 展示取消关注成功 toast。
7. 失败时保留“已关注”状态并展示错误 toast。

### 9.3 通用交互规则

- 目标用户是当前用户时不渲染按钮。
- loading 期间禁止按钮和菜单项重复触发。
- 点击菜单外部或按 Escape 关闭下拉框。
- 页面中同一目标用户出现多次时，共享或同步关注状态。
- toast 属于前端交互反馈，与站内 `NEW_FOLLOWER` 通知是两个独立概念。

---

## 10. 异常与边界

建议在 `ErrorDescriptionConstant` 增加：

```java
public static final String FOLLOW_ACTION_INVALID = "关注操作类型不合法";
public static final String FOLLOW_TARGET_NOT_FOUND = "关注用户不存在";
public static final String FOLLOW_SELF_NOT_ALLOWED = "不能关注自己";
public static final String FOLLOW_LIMIT_EXCEEDED = "关注数量不能超过5000";
```

| 场景 | 处理 |
|------|------|
| 未登录 | `NO_AUTH` |
| targetUserId 为空或不合法 | `PARAM_ERROR` |
| action 不是 1 或 2 | `PARAM_ERROR` + `FOLLOW_ACTION_INVALID` |
| 目标用户不存在或已删除 | `NOT_FOUND_ERROR` + `FOLLOW_TARGET_NOT_FOUND` |
| 关注自己 | `PARAM_ERROR` + `FOLLOW_SELF_NOT_ALLOWED` |
| 已达到 5000 后关注新用户 | `PARAM_ERROR` + `FOLLOW_LIMIT_EXCEEDED` |
| 重复关注 | 幂等成功 |
| 重复取消 | 幂等成功 |
| 重新关注 | 插入新的关系记录并增加计数 |
| 取消后计数异常为 0 | 使用 `GREATEST`，不允许出现负数 |
| 批量状态 ID 超过 100 个 | `PARAM_ERROR` + `PAGE_SIZE_EXCEEDED` |
| 列表 size 超过 50 | `PARAM_ERROR` + `PAGE_SIZE_EXCEEDED` |

用户删除流程必须同步处理关注关系，否则会产生无效列表项和错误计数。实现该功能时需将现有删除逻辑改为服务层事务：

- 删除该用户发出的全部关注关系，并减少对应用户的 `follower_count`。
- 删除该用户收到的全部关注关系，并减少对应用户的 `following_count`。
- 再删除或逻辑删除用户。

---

## 11. 数据一致性

### 11.1 事务边界

| 操作 | 所属事务 |
|------|----------|
| 关注关系插入或物理删除 | 主事务 |
| `following_count` 更新 | 主事务 |
| `follower_count` 更新 | 主事务 |
| 通知记录创建 | 主事务 |
| WebSocket 推送 | 事务提交后异步执行 |

任一数据库写入失败时，整个关注操作回滚。

### 11.2 计数更新

```sql
-- 关注者的关注数 +1
UPDATE `user`
SET following_count = following_count + 1
WHERE id = #{followerId};

-- 被关注者的粉丝数 +1
UPDATE `user`
SET follower_count = follower_count + 1
WHERE id = #{followedId};

-- 取消时防止负数
UPDATE `user`
SET following_count = GREATEST(following_count - 1, 0)
WHERE id = #{followerId};

UPDATE `user`
SET follower_count = GREATEST(follower_count - 1, 0)
WHERE id = #{followedId};
```

实现时优先使用 MyBatis-Plus `LambdaUpdateWrapper.setSql(...)` 完成原子增减。

### 11.3 对账

上线后可通过以下口径检查冗余计数：

```sql
SELECT u.id,
       u.following_count,
       COUNT(uf.followed_id) AS actual_following_count
FROM `user` u
LEFT JOIN user_follow uf
       ON uf.follower_id = u.id
GROUP BY u.id
HAVING u.following_count <> actual_following_count;
```

粉丝数对账将关联条件改为 `uf.followed_id = u.id`，并统计 `COUNT(uf.follower_id)`。

---

## 12. 测试清单

### 12.1 Service 测试

- 首次关注成功，关系和两个计数均正确。
- 重复关注成功，但计数和通知不重复增加。
- 取消关注后关系记录被物理删除，两个计数各减 1。
- 重复取消成功，计数不重复减少。
- 取消后重新关注插入新记录并生成新的关注时间，不重复通知。
- 不能关注自己。
- 不能关注不存在或已删除用户。
- 关注 4999 人后还能关注一人。
- 已关注 5000 人后不能关注新用户。
- 达到 5000 时重复关注已有用户仍幂等成功。
- 并发关注同一用户只增加一次计数。
- 并发关注不同用户不会突破 5000 上限。
- A 关注 B 与 B 关注 A 并发执行不死锁。
- 通知写入失败时关系和计数全部回滚。

### 12.2 查询测试

- 批量状态返回所有请求 ID，未关注项为 `false`。
- 批量状态能去重并限制最多 100 个 ID。
- 粉丝列表方向正确。
- 关注列表方向正确。
- 游标翻页无重复、无遗漏，关注时间相同时按列表用户 ID 稳定排序。
- 列表返回 `UserVO`，用户顺序与关注关系分页顺序一致。
- 用户信息返回正确的 `followerCount` 和 `followingCount`。
- 响应中的用户 ID、游标用户 ID 均序列化为字符串。

### 12.3 前端验收

- 三类入口均只在目标不是当前用户时显示关注按钮。
- 关注成功后按钮、下拉框和 toast 状态符合需求。
- 取消成功后按钮、下拉框和 toast 状态符合需求。
- 请求失败时 UI 状态不会被错误切换。
- 同页面相同作者的多个关注按钮状态保持一致。
- 点击粉丝数和关注数能进入对应列表。

---

## 13. 实施步骤

| 步骤 | 内容 | 优先级 |
|------|------|--------|
| 1 | 执行 `V1.0.6` SQL 迁移，创建关系表和用户计数字段 | P0 |
| 2 | 新增 `UserFollow`、Mapper、DTO、VO | P0 |
| 3 | 实现关注、取消、上限校验和冗余计数事务 | P0 |
| 4 | 实现批量状态、粉丝列表和关注列表 | P0 |
| 5 | 扩展 `User`、`UserVO` 及用户信息聚合逻辑 | P0 |
| 6 | 接入 `NEW_FOLLOWER` 通知和事务提交后推送 | P1 |
| 7 | 调整用户删除流程以清理关注关系和计数 | P1 |
| 8 | 接入前端按钮、下拉框、toast 和列表页面 | P1 |
| 9 | 补充 Service、Controller 和并发测试 | P1 |
