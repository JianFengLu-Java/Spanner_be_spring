# WebSocket 群聊接口文档

## 1. 连接信息
- 握手地址：`/ws`
- 协议：STOMP over WebSocket
- 应用前缀：`/app`
- 用户前缀：`/user`

## 2. 鉴权
- 在 `CONNECT` 头里传 token：
- `Authorization: Bearer <JWT>` 或 `token: <JWT>`
- 未通过鉴权会拒绝连接。

## 3. 客户端订阅通道
- 群消息：`/user/queue/group.messages`
- 群发送回执：`/user/queue/group.acks`
- 业务错误：`/user/queue/errors`

## 4. 发送群消息
- 发送目标：`/app/chat/group.send`
- 请求体：
```json
{
  "groupNo": "735662840120",
  "content": "晚上 8 点开会",
  "clientMessageId": "g_17370100001"
}
```
- 字段说明：
- `groupNo`：群号（必填）
- `content`：消息内容（必填）
- `clientMessageId`：客户端消息 ID（可选，建议传）

## 5. 下行数据格式

### 5.1 群消息 `/user/queue/group.messages`
```json
{
  "messageId": "8f536ea3-9201-418a-a622-f893d4878481",
  "groupNo": "735662840120",
  "from": "1000001",
  "content": "晚上 8 点开会",
  "clientMessageId": "g_17370100001",
  "sentAt": "2026-02-17T10:15:30.123"
}
```

### 5.2 群发送回执 `/user/queue/group.acks`
```json
{
  "clientMessageId": "g_17370100001",
  "messageId": "8f536ea3-9201-418a-a622-f893d4878481",
  "groupNo": "735662840120",
  "status": "SENT",
  "ackAt": "2026-02-17T10:15:30.123"
}
```

### 5.3 业务错误 `/user/queue/errors`
```json
{
  "code": "GROUP_PERMISSION_DENIED",
  "message": "你不在该群",
  "clientMessageId": "g_17370100001",
  "at": "2026-02-17T10:15:30.123"
}
```

## 6. 业务规则
- 只有群成员才能发送群消息。
- 群消息会广播给该群所有成员（包含发送者）。
- 消息会持久化到数据库，可通过 REST 接口查询历史。

## 7. 前端示例（stompjs）
```javascript
client.subscribe("/user/queue/group.messages", (frame) => {
  const msg = JSON.parse(frame.body);
  console.log("group message", msg);
});

client.subscribe("/user/queue/group.acks", (frame) => {
  const ack = JSON.parse(frame.body);
  console.log("group ack", ack);
});

client.publish({
  destination: "/app/chat/group.send",
  body: JSON.stringify({
    groupNo: "735662840120",
    content: "晚上 8 点开会",
    clientMessageId: "g_17370100001"
  })
});
```
