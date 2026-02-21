# 1 对 1 WebRTC MVP 方案与 API 协议

## 1. 目标与范围

目标：以最小成本上线可用的 1 对 1 音视频通话。

MVP 范围：
- 仅支持 1 对 1（每个会话最多 2 人）
- 支持音频 + 摄像头视频
- 支持基础呼叫流程：创建会话、加入、协商、挂断
- 暂不支持：多人房间、屏幕共享、云录制、转写

## 2. 技术架构（MVP）

- 客户端：Web/iOS/Android（WebRTC）
- 信令：Spring Boot + WebSocket
- 网络穿透：STUN/TURN（coturn）
- 存储：
  - MySQL（会话记录）
  - Redis（可选，管理在线状态/会话临时态）

> 说明：1 对 1 场景下可先采用 P2P（配合 TURN），后续再平滑升级 SFU。

## 3. 关键时序

1. A 创建会话，获得 `session_id`
2. A/B 分别获取 `join_token` 与 `ice_servers`
3. A/B 连接 WebSocket 信令
4. 双方加入会话后，发起 `offer/answer`
5. 双方持续交换 `ice_candidate`
6. 建立 `RTCPeerConnection`，进入通话
7. 任一方挂断，发送 `leave`，会话结束

## 4. REST API

### 4.1 通用约定
- Base URL：`/api/v1`
- Auth：`Authorization: Bearer <access_token>`
- 返回格式：

```json
{
  "code": 0,
  "message": "ok",
  "request_id": "req_xxx",
  "data": {}
}
```

### 4.2 创建会话
`POST /api/v1/call/sessions`

Request:
```json
{
  "caller_user_id": "u_1001",
  "callee_user_id": "u_2001",
  "media": ["audio", "video"]
}
```

Response:
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "session_id": "cs_12345",
    "status": "ringing",
    "expire_at": "2026-02-20T10:30:00Z"
  }
}
```

### 4.3 获取入会凭证
`POST /api/v1/call/sessions/{session_id}/token`

Request:
```json
{
  "device_id": "web_abc",
  "client_version": "1.0.0"
}
```

Response:
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "join_token": "jwt_xxx",
    "ws_url": "wss://rtc.example.com/ws",
    "ice_servers": [
      { "urls": "stun:stun.example.com:3478" },
      {
        "urls": "turn:turn.example.com:3478",
        "username": "temp_user",
        "credential": "temp_pass"
      }
    ]
  }
}
```

### 4.4 查询会话状态
`GET /api/v1/call/sessions/{session_id}`

Response:
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "session_id": "cs_12345",
    "status": "connected",
    "caller_user_id": "u_1001",
    "callee_user_id": "u_2001",
    "started_at": "2026-02-20T10:05:00Z"
  }
}
```

状态建议：
- `ringing`
- `connected`
- `ended`
- `timeout`
- `rejected`

### 4.5 挂断会话
`POST /api/v1/call/sessions/{session_id}/leave`

Request:
```json
{
  "reason": "user_hangup"
}
```

## 5. WebSocket 信令协议

连接：
`wss://rtc.example.com/ws?join_token=xxx`

统一消息结构：
```json
{
  "event": "string",
  "request_id": "req_001",
  "ts": 1760000000000,
  "payload": {}
}
```

### 5.1 客户端 -> 服务端

#### `join`
```json
{
  "event": "join",
  "request_id": "req_join",
  "payload": {
    "session_id": "cs_12345",
    "user_id": "u_1001"
  }
}
```

#### `offer`
```json
{
  "event": "offer",
  "request_id": "req_offer",
  "payload": {
    "session_id": "cs_12345",
    "sdp": "v=0..."
  }
}
```

#### `answer`
```json
{
  "event": "answer",
  "request_id": "req_answer",
  "payload": {
    "session_id": "cs_12345",
    "sdp": "v=0..."
  }
}
```

#### `ice_candidate`
```json
{
  "event": "ice_candidate",
  "request_id": "req_ice_1",
  "payload": {
    "session_id": "cs_12345",
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

#### `leave`
```json
{
  "event": "leave",
  "request_id": "req_leave",
  "payload": {
    "session_id": "cs_12345",
    "reason": "user_hangup"
  }
}
```

### 5.2 服务端 -> 客户端

#### `join_ack`
```json
{
  "event": "join_ack",
  "request_id": "req_join",
  "payload": {
    "session_id": "cs_12345",
    "role": "caller",
    "peer_joined": false
  }
}
```

#### `peer_joined`
```json
{
  "event": "peer_joined",
  "payload": {
    "session_id": "cs_12345",
    "user_id": "u_2001"
  }
}
```

#### `offer`（转发）
```json
{
  "event": "offer",
  "payload": {
    "session_id": "cs_12345",
    "from_user_id": "u_1001",
    "sdp": "v=0..."
  }
}
```

#### `answer`（转发）
```json
{
  "event": "answer",
  "payload": {
    "session_id": "cs_12345",
    "from_user_id": "u_2001",
    "sdp": "v=0..."
  }
}
```

#### `ice_candidate`（转发）
```json
{
  "event": "ice_candidate",
  "payload": {
    "session_id": "cs_12345",
    "from_user_id": "u_2001",
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

#### `peer_left`
```json
{
  "event": "peer_left",
  "payload": {
    "session_id": "cs_12345",
    "user_id": "u_2001",
    "reason": "user_hangup"
  }
}
```

#### `error`
```json
{
  "event": "error",
  "request_id": "req_offer",
  "payload": {
    "code": 40012,
    "message": "invalid sdp"
  }
}
```

## 6. 错误码建议

- `40001` 参数非法
- `40003` 鉴权失败
- `40004` 会话不存在
- `40009` 会话人数已满（>2）
- `40012` SDP 不合法
- `40029` 请求频率超限
- `50000` 服务内部错误

## 7. 上线前最小保障

- 必配 TURN，且使用临时凭证
- WebSocket 心跳（如 15s 一次）+ 超时踢线
- 断线重连（30s 内可恢复）
- 会话超时回收（如 ringing 超时 60s）
- 通话质量日志（首帧时间、丢包率、重传率、卡顿次数）

## 8. Spring 模块落地建议（MVP）

- `call-session-controller`：REST 会话接口
- `signal-websocket-handler`：信令事件收发
- `signal-router-service`：事件转发到对端
- `ice-config-service`：TURN 临时凭证签发
- `call-record-service`：通话状态与记录落库

## 9. 建议迭代顺序

1. 打通 `join -> offer -> answer -> ice_candidate -> connected`
2. 完成 `leave` 与异常断开收敛
3. 增加弱网策略（优先音频）
4. 补充静音/关视频状态同步
5. 再扩展到多人或 SFU
