# WebSocket 云文档协作协议

## 1. 连接信息
- 握手地址：`/ws`
- 协议：STOMP over WebSocket
- 应用前缀：`/app`
- 用户前缀：`/user`

## 2. 鉴权
- STOMP `CONNECT` Header：
- `Authorization: Bearer <JWT>` 或 `token: <JWT>`
- 未通过鉴权会拒绝连接。
- 订阅限制：仅允许 `/user/queue/**`。

## 3. 客户端订阅通道
- 协作事件：`/user/queue/cloud-docs.events`
- 协作回执：`/user/queue/cloud-docs.acks`
- 业务错误：`/user/queue/errors`

## 4. 客户端发送目标
- 进入协作：`/app/cloud-docs.join`
- 离开协作：`/app/cloud-docs.leave`
- 游标更新：`/app/cloud-docs.cursor`
- 内容变更广播：`/app/cloud-docs.patch`

## 5. 入参协议

### 5.1 join
```json
{
  "docId": "doc_20260220_000001"
}
```

### 5.2 leave
```json
{
  "docId": "doc_20260220_000001"
}
```

### 5.3 cursor
```json
{
  "docId": "doc_20260220_000001",
  "anchor": 12,
  "head": 18
}
```

### 5.4 patch
```json
{
  "docId": "doc_20260220_000001",
  "baseVersion": 8,
  "opId": "op_17370100001",
  "opType": "replace",
  "payload": "{\"path\":\"/content/0\",\"value\":\"hello\"}"
}
```

## 6. 下行协议

### 6.1 协作事件 `/user/queue/cloud-docs.events`
```json
{
  "eventType": "content.patch",
  "docId": "doc_20260220_000001",
  "from": "10001",
  "at": "2026-02-20T12:00:00Z",
  "data": {
    "baseVersion": 8,
    "serverVersion": 9,
    "opId": "op_17370100001",
    "opType": "replace",
    "payload": "{\"path\":\"/content/0\",\"value\":\"hello\"}"
  }
}
```

事件类型：
- `presence.snapshot`：在线成员快照
- `presence.join`：成员进入
- `presence.leave`：成员离开
- `cursor.update`：游标变化
- `content.patch`：内容变更广播

`presence.snapshot.data` 示例：
```json
{
  "members": [
    {
      "account": "10001",
      "cursor": {
        "anchor": 12,
        "head": 18
      }
    }
  ],
  "onlineCount": 1,
  "serverVersion": 9
}
```

### 6.2 协作回执 `/user/queue/cloud-docs.acks`
```json
{
  "action": "patch",
  "docId": "doc_20260220_000001",
  "opId": "op_17370100001",
  "baseVersion": 8,
  "serverVersion": 9,
  "status": "APPLIED",
  "reason": null,
  "at": "2026-02-20T12:00:00Z"
}
```

`status` 枚举：
- `APPLIED`：服务端已接收并分配 `serverVersion`，可从待发队列移除。
- `CONFLICT`：`baseVersion` 与服务端版本不一致，需要重拉并重放本地未确认操作。
- `DUPLICATE`：同 `opId` 重复提交，可直接忽略。
- `REJECTED`：请求参数非法（如 `baseVersion` 缺失）。

### 6.3 业务错误 `/user/queue/errors`
```json
{
  "code": "CLOUD_DOC_WS_PATCH_FAILED",
  "message": "无权限协作该文档",
  "clientMessageId": "op_17370100001",
  "at": "2026-02-20T12:00:00"
}
```

## 7. 权限规则
- 文档 owner 可加入协作。
- 被该文档有效分享（`ACTIVE` 且未过期）并且 `shareMode=COLLAB` 的好友可加入协作。
- `READONLY` 分享不能加入协作 WS。
- 其他用户会收到错误消息。

## 8. 说明
- `patch` 走“服务端顺序版本线”：
- 仅当 `baseVersion == 当前 serverVersion` 才会 `APPLIED`。
- `APPLIED` 后返回新的 `serverVersion`，并广播 `content.patch`。
- 持久化仍走 REST：`PUT /cloud-docs/{docId}`，其 `baseVersion` 需使用最新 `serverVersion`。
- 服务端 `save` 已按“数据库版本 vs 协作版本”取最大值校验，减少多人协同时的误冲突。

## 9. 前端接入示例
```javascript
client.subscribe('/user/queue/cloud-docs.events', frame => {
  const event = JSON.parse(frame.body)
  console.log('collab event', event)
})

client.subscribe('/user/queue/cloud-docs.acks', frame => {
  const ack = JSON.parse(frame.body)
  console.log('collab ack', ack)
})

client.publish({
  destination: '/app/cloud-docs.join',
  body: JSON.stringify({ docId: 'doc_20260220_000001' })
})

client.publish({
  destination: '/app/cloud-docs.patch',
  body: JSON.stringify({
    docId: 'doc_20260220_000001',
    baseVersion: 8,
    opId: 'op_17370100001',
    opType: 'replace',
    payload: '{"path":"/content/0","value":"hello"}'
  })
})
```
