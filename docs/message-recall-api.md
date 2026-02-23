# 消息撤回 API 文档（前端对接）

## 1. 功能说明
- 支持私聊/群聊消息撤回。
- 仅允许撤回“自己发送”的消息。
- 超过 24 小时（按 `sentAt + 24h`）不可撤回。

## 2. REST 接口
- 方法：`POST`
- 路径：`/messages/{messageId}/recall`
- 鉴权：`Authorization: Bearer <token>`

### 2.1 请求体
```json
{
  "messageType": "PRIVATE",
  "groupNo": "12345678"
}
```

字段说明：
1. `messageType`：必填，`PRIVATE` 或 `GROUP`
2. `groupNo`：可选，仅群消息时建议传；若传入需与消息所属群一致

### 2.2 成功响应示例
```json
{
  "code": 200,
  "status": "OK",
  "message": "撤回成功",
  "data": {
    "messageId": "e5fa90e0-a64a-4ab7-a1d0-4c1945f2f2f6",
    "messageType": "GROUP",
    "groupNo": "12345678",
    "from": "alice",
    "to": null,
    "recalled": true,
    "recalledAt": "2026-02-23T20:30:00",
    "recallDeadlineAt": "2026-02-24T15:12:00"
  }
}
```

## 3. 错误码
1. `401 UNAUTHORIZED`：未登录
2. `400 RECALL_INVALID_PARAM`：参数错误（如 `messageType` 非法、`groupNo` 不匹配）
3. `403 MESSAGE_RECALL_FORBIDDEN`：尝试撤回他人消息
4. `404 MESSAGE_NOT_FOUND`：消息不存在
5. `409 MESSAGE_RECALL_CONFLICT`：消息已撤回或超过 24 小时

## 4. 历史消息结构变更
以下消息对象新增字段：
1. `recalled`: `Boolean`
2. `recalledAt`: `LocalDateTime`

涉及接口：
1. `GET /messages/offline`
2. `GET /messages/history/{friendAccount}`
3. `GET /groups/{groupNo}/messages/history`

## 5. WebSocket 撤回事件
撤回成功后，服务端会推送实时事件，前端可据此直接替换本地消息状态。

### 5.1 私聊事件
- 订阅：`/user/queue/messages.recalled`

### 5.2 群聊事件
- 订阅：`/user/queue/group.messages.recalled`

### 5.3 事件体示例
```json
{
  "eventType": "MESSAGE_RECALLED",
  "messageType": "PRIVATE",
  "messageId": "e5fa90e0-a64a-4ab7-a1d0-4c1945f2f2f6",
  "groupNo": null,
  "from": "alice",
  "to": "bob",
  "recalled": true,
  "recalledAt": "2026-02-23T20:30:00"
}
```

## 6. 前端处理建议
1. 收到撤回事件后，以 `messageId` 定位本地消息并将其渲染为“该消息已撤回”。
2. 拉取历史时如 `recalled=true`，忽略原始 `content` 展示撤回态。
3. 撤回按钮展示条件建议：`message.from == 当前用户 && now <= sentAt + 24h`。
