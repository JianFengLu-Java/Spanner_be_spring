# 好友请求 WebSocket 事件文档

## 连接与鉴权
- STOMP 端点: `/ws`
- `CONNECT` 必须带 token
- 支持 Header:
- `Authorization: Bearer <JWT>`
- `token: <JWT>`

## 订阅限制
- 服务端只允许订阅: `/user/queue/**`
- 好友请求主通道: `/user/queue/friend-requests`
- 可选总线通道: `/user/queue/friend-events`（同样事件，便于后续扩展）

## 事件模型
```json
{
  "eventId": "2e88b26c-7342-4d97-a736-c0ec74a6cc8e",
  "eventType": "FRIEND_REQUEST_CREATED",
  "occurredAt": "2026-02-12T10:00:00Z",
  "requestId": "fr_1739349700000_1a2b3c4d",
  "fromAccount": "alice",
  "fromName": "Alice",
  "fromAvatarUrl": "https://example.com/a.png",
  "toAccount": "bob",
  "status": "PENDING",
  "unreadPendingCount": 3
}
```

## eventType
- `FRIEND_REQUEST_CREATED`
- `FRIEND_REQUEST_ACCEPTED`
- `FRIEND_REQUEST_REJECTED`
- `FRIEND_REQUEST_CANCELED`
- `FRIEND_REQUEST_EXPIRED`

## 触发时机
- `POST /friends/apply` 成功后:
- 推送给接收方 `toAccount`
- `POST /friends/accept`、`POST /friends/reject`、`POST /friends/cancel` 成功后:
- 推送给双方（申请方、处理方）
- 请求过期（状态转 `EXPIRED`）后:
- 推送给双方（申请方、接收方）

## 可靠性说明
- 事件在事务提交后发送，避免回滚造成假通知
- 前端重连后应调用 `GET /friends/requests/pending` 做兜底同步

## 错误与限流
- 申请接口已加基础频控，过快重复申请返回:
- `429`，`errorCode=FRIEND_REQUEST_RATE_LIMITED`
