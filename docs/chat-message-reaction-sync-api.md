# 聊天消息表情回复（Reaction）同步 API 文档

## 1. 概览

- 目标：支持消息表情回复的云端同步（多端一致）
- 能力：
1. 对单条消息添加/取消某个表情（toggle）
2. 获取单条消息完整 reactions 快照
3. 通过 WebSocket 实时推送 reactions 更新
4. 私聊场景新增表情时，给好友发送系统通知（`XXX 回复了信息`）

Base URL（示例）：
- `http://<host>:8080`

鉴权：
- 所有接口需登录态（JWT）

---

## 2. 数据结构

### 2.1 Reaction

```json
{
  "key": "u:👍",
  "emoji": "👍",
  "imageUrl": null,
  "count": 2,
  "userIds": ["10001", "10002"],
  "updatedAt": "2026-02-21T13:53:00Z"
}
```

字段说明：
- `key`：消息内唯一表情键（建议 `u:<unicode>` / `t:<token>` / `i:<url-hash>`）
- `emoji` / `imageUrl`：至少一个非空
- `count`：服务端计算，恒等于 `userIds.length`
- `updatedAt`：UTC 时间

### 2.2 MessageReactionSnapshot

```json
{
  "chatId": 20010001,
  "messageId": "msg_20260221_0001",
  "serverMessageId": "msg_20260221_0001",
  "clientMessageId": "c_abc123",
  "reactions": []
}
```

---

## 3. REST API

## 3.1 Toggle Reaction

- 方法：`PUT`
- 路径：`/messages/{messageId}/reactions/toggle`
- 说明：当前用户对某个表情做切换（已点则取消，未点则添加）

Path 参数：
- `messageId`：建议传服务端消息 ID；也支持本地消息主键字符串（回退定位）

请求体：

```json
{
  "chatId": 20010001,
  "serverMessageId": "msg_20260221_0001",
  "clientMessageId": "c_abc123",
  "reaction": {
    "key": "u:👍",
    "emoji": "👍",
    "imageUrl": null
  },
  "operatorId": "10001",
  "requestId": "req_20260221_001"
}
```

说明：
- `chatId`：必填
- `requestId`：必填，用于幂等
- `operatorId`：可传，服务端以 token 用户为准
- 消息定位优先级：`serverMessageId` -> `path.messageId` -> `clientMessageId`

成功响应：

```json
{
  "code": 200,
  "status": "OK",
  "message": "success",
  "data": {
    "chatId": 20010001,
    "messageId": "msg_20260221_0001",
    "serverMessageId": "msg_20260221_0001",
    "clientMessageId": "c_abc123",
    "reactions": [
      {
        "key": "u:👍",
        "emoji": "👍",
        "imageUrl": null,
        "count": 2,
        "userIds": ["10001", "10002"],
        "updatedAt": "2026-02-21T13:53:00Z"
      }
    ]
  }
}
```

---

## 3.2 Get Reactions Snapshot

- 方法：`GET`
- 路径：`/messages/{messageId}/reactions`
- 用途：断线重连/补偿同步时拉取该消息最新 reactions

Query 参数：
- `chatId`：必填
- `serverMessageId`：可选（建议传）
- `clientMessageId`：可选

示例：
- `/messages/msg_20260221_0001/reactions?chatId=20010001&serverMessageId=msg_20260221_0001`

成功响应：
- 与 3.1 的 `data` 结构一致（完整快照）

---

## 4. WebSocket 实时事件

订阅地址：
- `/user/queue/message.reactions.updated`

事件体：

```json
{
  "eventType": "message.reactions.updated",
  "chatId": 20010001,
  "messageId": "msg_20260221_0001",
  "serverMessageId": "msg_20260221_0001",
  "updatedAt": "2026-02-21T13:53:00Z",
  "reactions": []
}
```

前端处理建议：
- 收到事件后，按消息 ID 定位消息
- 用事件内 `reactions` **整包覆盖**本地 `message.reactions`
- `updatedAt` 旧于本地时可丢弃（防乱序）

---

## 5. 系统通知（私聊）

当私聊消息被“新增 reaction”时，服务端会给对方发送系统消息：
- 通道：`/user/queue/messages`
- from：`SYSTEM`
- content 格式（JSON 字符串）：
  `{"messageContent":"消息正文","reaction":"表情","operatorName":"用户名","operatorAvatarUrl":"头像地址"}`
- content 示例：
  `{"messageContent":"今晚八点开会","reaction":"👍","operatorName":"用户A","operatorAvatarUrl":"https://cdn.example.com/avatar/a.png"}`

说明：
- 仅“新增”触发，取消不触发
- 当前仅私聊触发，群聊不触发
- `messageContent` 返回消息正文全量内容（不截断）
- 表情显示优先级：`emoji` -> `自定义表情` -> `reaction.key`

---

## 6. 错误码

- `400 REACTION_INVALID_PARAM`：参数缺失/非法
- `401 UNAUTHORIZED`：未登录
- `403 MESSAGE_FORBIDDEN`：无权操作该消息
- `404 MESSAGE_NOT_FOUND`：消息不存在
- `409 REACTION_STATE_CONFLICT`：状态冲突（例如 clientMessageId 歧义）
- `500 INTERNAL_ERROR`：服务端异常

---

## 7. 联调检查清单

1. A 端 toggle 表情后，A/B 两端都收到 `/user/queue/message.reactions.updated`
2. A/B 两端同一消息的 reactions 一致
3. 刷新后调用 GET，快照与服务端一致
4. 重复提交同一 `requestId` 不重复计数
5. 私聊新增表情时，对方收到 `SYSTEM` 消息通知
