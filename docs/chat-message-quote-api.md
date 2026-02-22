# 消息引用功能 API 文档（前端对接）

## 1. 功能说明

消息发送时支持携带 `quote` 字段，表示“引用某条历史消息”。

适用范围：
1. 私聊发送（WebSocket）
2. 群聊发送（WebSocket）
3. 私聊离线拉取（HTTP）
4. 私聊历史查询（HTTP）
5. 群聊历史查询（HTTP）

---

## 2. 数据结构

### 2.1 MessageQuote

```json
{
  "messageId": "msg_123",
  "from": "10001",
  "content": "<p>被引用消息内容</p>"
}
```

字段说明：
1. `messageId`：被引用消息 ID，必填（发送时）
2. `from`：被引用消息发送方账号，可选
3. `content`：被引用消息内容快照，可选

---

## 3. 发送接口（WebSocket）

STOMP Endpoint：
1. 私聊发送：`/app/chat/private.send`
2. 群聊发送：`/app/chat/group.send`

### 3.1 私聊发送请求体

```json
{
  "to": "10002",
  "content": "<p>这是新消息</p>",
  "clientMessageId": "c_001",
  "quote": {
    "messageId": "msg_old_001",
    "from": "10002",
    "content": "<p>上一条消息</p>"
  }
}
```

### 3.2 群聊发送请求体

```json
{
  "groupNo": "G10001",
  "content": "<p>这是群消息</p>",
  "clientMessageId": "cg_001",
  "quote": {
    "messageId": "msg_old_101",
    "from": "10003",
    "content": "<p>群里的一条历史消息</p>"
  }
}
```

校验规则：
1. `quote` 为可选
2. `quote` 存在时，`quote.messageId` 必填
3. `quote.from/content` 可空

---

## 4. 下行消息结构（WebSocket）

### 4.1 私聊下行 `/user/queue/messages`

```json
{
  "messageId": "msg_new_001",
  "from": "10001",
  "to": "10002",
  "content": "<p>这是新消息</p>",
  "quote": {
    "messageId": "msg_old_001",
    "from": "10002",
    "content": "<p>上一条消息</p>"
  },
  "clientMessageId": "c_001",
  "sentAt": "2026-02-22T10:00:00"
}
```

### 4.2 群聊下行 `/user/queue/group.messages`

```json
{
  "messageId": "msg_new_101",
  "groupNo": "G10001",
  "from": "10001",
  "content": "<p>这是群消息</p>",
  "quote": {
    "messageId": "msg_old_101",
    "from": "10003",
    "content": "<p>群里的一条历史消息</p>"
  },
  "clientMessageId": "cg_001",
  "sentAt": "2026-02-22T10:00:00"
}
```

---

## 5. 历史/离线拉取返回

以下接口返回的消息对象已支持 `quote`：
1. `GET /messages/offline`
2. `GET /messages/history/{friendAccount}`
3. `GET /groups/{groupNo}/messages/history`

消息对象中的 `quote` 结构与 `MessageQuote` 一致。

---

## 6. 前端渲染建议

1. `quote == null`：按普通消息渲染
2. `quote != null`：显示引用卡片（被引用发送方 + 被引用内容摘要）
3. 若 `quote.content` 为富文本，建议先做安全渲染策略（白名单）

---

## 7. 兼容性说明

1. 老客户端不传 `quote` 仍可正常发送
2. 新增字段均为向后兼容，不影响现有 `content` 消息流程
