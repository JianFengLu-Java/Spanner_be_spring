# 音视频通话 API 文档

## 1. 通用
- Base: 与现有服务一致（如 `http://localhost:8080`）
- 鉴权: `Authorization: Bearer <JWT>`
- 返回结构:
```json
{
  "code": 200,
  "status": "success",
  "message": "success",
  "data": {}
}
```

## 2. 状态与枚举
- `type`: `VIDEO|AUDIO`
- `status`: `RINGING|ANSWERED|CONNECTING|CONNECTED|REJECTED|CANCELED|NO_ANSWER|BUSY|ENDED|FAILED`
- `signalType`: `OFFER|ANSWER|ICE_CANDIDATE|RENEGOTIATE`

## 3. REST 接口

### 3.1 发起通话
- `POST /calls`
- 请求体:
```json
{
  "requestId": "req_20260223_000001",
  "calleeAccount": "10002",
  "type": "VIDEO"
}
```
- `data` 示例:
```json
{
  "callId": "call_xxx",
  "status": "RINGING",
  "expiresAt": "2026-02-23T08:30:45Z"
}
```

### 3.2 接听
- `POST /calls/{callId}/accept`
- 请求体:
```json
{
  "requestId": "req_20260223_000010"
}
```

### 3.3 拒绝
- `POST /calls/{callId}/reject`
- 请求体:
```json
{
  "requestId": "req_20260223_000011",
  "reason": "REJECTED_BY_USER"
}
```

### 3.4 取消
- `POST /calls/{callId}/cancel`
- 请求体:
```json
{
  "requestId": "req_20260223_000012"
}
```

### 3.5 挂断
- `POST /calls/{callId}/end`
- 请求体:
```json
{
  "requestId": "req_20260223_000013",
  "reason": "HANGUP"
}
```

### 3.6 查询会话
- `GET /calls/{callId}`
- `data` 为完整 `CallSession`：
```json
{
  "callId": "call_xxx",
  "type": "VIDEO",
  "status": "CONNECTED",
  "callerAccount": "10001",
  "callerName": "张三",
  "callerAvatar": "https://...",
  "calleeAccount": "10002",
  "calleeName": "李四",
  "calleeAvatar": "https://...",
  "channelId": "private:10001:10002",
  "startedAt": "2026-02-23T08:30:00Z",
  "expiresAt": "2026-02-23T08:30:45Z",
  "answeredAt": "2026-02-23T08:30:06Z",
  "connectedAt": "2026-02-23T08:30:09Z",
  "endedAt": null,
  "endReason": null,
  "durationSeconds": 0
}
```

### 3.7 上行信令
- `POST /calls/{callId}/signals`
- Offer 示例:
```json
{
  "requestId": "req_20260223_000020",
  "signalType": "OFFER",
  "to": "10002",
  "sdp": "v=0..."
}
```
- ICE 示例:
```json
{
  "requestId": "req_20260223_000021",
  "signalType": "ICE_CANDIDATE",
  "to": "10002",
  "candidate": "candidate:...",
  "sdpMid": "0",
  "sdpMLineIndex": 0
}
```
- 返回:
```json
{
  "accepted": true
}
```

### 3.8 保活
- `POST /calls/{callId}/heartbeat`
- 请求体:
```json
{
  "requestId": "req_20260223_000030"
}
```

### 3.9 通话记录
- `GET /calls/history?page=1&size=20`
- 返回字段：`items/page/size/total/totalPages/hasMore`

### 3.10 ICE Server
- `GET /rtc/ice-servers`
- 示例:
```json
{
  "servers": [
    {
      "urls": ["stun:stun.l.google.com:19302"]
    }
  ]
}
```

## 4. WebSocket 事件

### 4.1 `/user/queue/calls`
- `incoming.call`
```json
{
  "event": "incoming.call",
  "payload": {
    "callId": "call_xxx",
    "from": "10001",
    "fromName": "张三",
    "fromAvatar": "https://...",
    "type": "video",
    "chatId": 10001,
    "createdAt": "2026-02-23T08:30:00Z"
  }
}
```
- `call.answered`
```json
{
  "event": "call.answered",
  "payload": {
    "callId": "call_xxx",
    "answeredBy": "10002",
    "answeredAt": "2026-02-23T08:30:06Z"
  }
}
```
- `call.ended`
```json
{
  "event": "call.ended",
  "payload": {
    "callId": "call_xxx",
    "status": "ENDED",
    "endReason": "HANGUP",
    "endedAt": "2026-02-23T08:38:12Z",
    "durationSeconds": 486
  }
}
```

### 4.2 `/user/queue/call-signals`
```json
{
  "event": "call.signal",
  "payload": {
    "callId": "call_xxx",
    "signalType": "OFFER",
    "from": "10001",
    "to": "10002",
    "sdp": "v=0...",
    "candidate": null,
    "sdpMid": null,
    "sdpMLineIndex": null,
    "createdAt": "2026-02-23T08:30:02Z"
  }
}
```

## 5. 错误码
- `400 CALL_INVALID_PARAM`
- `401 UNAUTHORIZED`
- `403 CALL_FORBIDDEN`
- `404 CALL_NOT_FOUND`
- `409 CALL_STATE_CONFLICT`
- `409 CALLEE_BUSY`
- `410 CALL_EXPIRED`
- `422 RTC_SIGNAL_INVALID`
- `500 INTERNAL_ERROR`
