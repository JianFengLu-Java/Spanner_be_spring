# 音视频流通话可落地解决方案（含 API 协议）

## 1. 方案选型（落地优先）

### 1.1 总体架构
- 客户端：Web / iOS / Android（统一使用 WebRTC）
- 接入层：API Gateway + Auth
- 信令层：Spring Boot + WebSocket（房间、成员、offer/answer/ice、控制消息）
- 媒体层：SFU（推荐 Janus / mediasoup / LiveKit 之一）
- 穿透层：STUN/TURN（coturn，公网场景必备）
- 数据层：
  - MySQL：用户、房间、会话记录
  - Redis：房间在线状态、分布式会话、限流
- 可观测：Prometheus + Grafana + ELK

### 1.2 为什么选 SFU（而非 MCU / 纯 P2P）
- 多人通话稳定：终端上行只发一路，节省带宽和电量
- 服务端不混流：延迟更低，扩展性更好
- 易扩展：支持录制、旁路转推、订阅控制（大流/小流）

## 2. 核心业务流程

1. 用户登录，获取 `access_token`
2. 创建房间或加入房间（REST）
3. 客户端连接信令 WebSocket（携带 token）
4. 客户端发送 `join`，服务端返回房间成员和媒体能力
5. WebRTC 协商（offer/answer + ICE）通过信令交换
6. 客户端向 SFU 发布音视频流，并订阅其他成员流
7. 通话中上报网络状态，支持静音、关摄像头、屏幕共享
8. 离会/结束房间，写入通话记录

## 3. API 协议文档（草案）

### 3.1 通用约定
- Base URL：`/api/v1`
- 鉴权：`Authorization: Bearer <JWT>`
- 时间：ISO8601（UTC）
- 幂等：创建类接口支持 `Idempotency-Key`

统一返回结构：

```json
{
  "code": 0,
  "message": "ok",
  "request_id": "8f7c...",
  "data": {}
}
```

说明：`code=0` 表示成功，非 `0` 表示失败。

### 3.2 REST API

#### 1) 创建房间
`POST /api/v1/rooms`

Request：

```json
{
  "type": "group",
  "max_participants": 16,
  "recording_enabled": false
}
```

Response：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "room_id": "r_123",
    "creator_user_id": "u_1",
    "expire_at": "2026-02-20T10:00:00Z"
  }
}
```

#### 2) 获取房间信息
`GET /api/v1/rooms/{room_id}`

Response：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "room_id": "r_123",
    "type": "group",
    "online_count": 5,
    "status": "active"
  }
}
```

#### 3) 申请入会票据（Join Token）
`POST /api/v1/rooms/{room_id}/join-token`

Request：

```json
{
  "device_id": "ios_xxx",
  "client_version": "1.2.0"
}
```

Response：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "join_token": "jwt_xxx",
    "ws_url": "wss://rtc.example.com/ws",
    "ice_servers": [
      { "urls": "stun:stun.example.com:3478" },
      { "urls": "turn:turn.example.com:3478", "username": "u", "credential": "p" }
    ],
    "sfu": {
      "region": "ap-east-1",
      "endpoint": "wss://sfu.example.com"
    }
  }
}
```

#### 4) 离开房间（业务态）
`POST /api/v1/rooms/{room_id}/leave`

Request：

```json
{
  "reason": "user_hangup"
}
```

#### 5) 查询通话记录
`GET /api/v1/calls/{call_id}`

Response：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "call_id": "c_1001",
    "room_id": "r_123",
    "start_at": "2026-02-20T09:00:00Z",
    "end_at": "2026-02-20T09:30:00Z",
    "participants": 6
  }
}
```

### 3.3 WebSocket 信令协议

连接地址：
`wss://rtc.example.com/ws?join_token=xxx`

通用消息格式：

```json
{
  "event": "string",
  "request_id": "uuid",
  "ts": 1700000000000,
  "payload": {}
}
```

#### 客户端 -> 服务端事件

1) `join`

```json
{
  "event": "join",
  "request_id": "req1",
  "payload": {
    "room_id": "r_123",
    "user_id": "u_1",
    "display_name": "Tom"
  }
}
```

2) `publish_offer`

```json
{
  "event": "publish_offer",
  "request_id": "req2",
  "payload": {
    "sdp": "v=0...",
    "media": ["audio", "video"]
  }
}
```

3) `subscribe_offer`

```json
{
  "event": "subscribe_offer",
  "request_id": "req3",
  "payload": {
    "target_user_id": "u_2",
    "stream_id": "s_200"
  }
}
```

4) `trickle_ice`

```json
{
  "event": "trickle_ice",
  "request_id": "req4",
  "payload": {
    "candidate": "candidate:...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

5) `mute_track`

```json
{
  "event": "mute_track",
  "request_id": "req5",
  "payload": {
    "kind": "audio",
    "muted": true
  }
}
```

#### 服务端 -> 客户端事件

1) `join_ack`

```json
{
  "event": "join_ack",
  "request_id": "req1",
  "payload": {
    "room_id": "r_123",
    "participants": [
      { "user_id": "u_2", "display_name": "Jerry" }
    ]
  }
}
```

2) `publish_answer`

```json
{
  "event": "publish_answer",
  "request_id": "req2",
  "payload": {
    "sdp": "v=0..."
  }
}
```

3) `participant_joined` / `participant_left`

```json
{
  "event": "participant_joined",
  "payload": {
    "user_id": "u_3",
    "display_name": "Alice"
  }
}
```

4) `track_state_changed`

```json
{
  "event": "track_state_changed",
  "payload": {
    "user_id": "u_2",
    "kind": "video",
    "muted": true
  }
}
```

5) `error`

```json
{
  "event": "error",
  "request_id": "req2",
  "payload": {
    "code": 40012,
    "message": "sdp invalid"
  }
}
```

### 3.4 错误码建议
- `40001` 参数非法
- `40003` 鉴权失败
- `40004` 房间不存在
- `40009` 房间已满
- `40012` SDP 不合法
- `40029` 频率超限
- `50000` 服务内部错误

## 4. 非功能要求（上线必备）

- 音频优先：弱网自动降级视频码率/分辨率，优先保证声音不断
- 目标指标：
  - 首帧 < 2s
  - 端到端延迟 < 400ms（互动场景）
  - 丢包 15% 内可恢复可懂度
- 安全：
  - JWT 短期有效（例如 10 分钟）
  - DTLS-SRTP 全链路加密
  - TURN 临时凭证（动态签发）
- 扩展：
  - 房间维度分片
  - SFU 节点按区域调度
  - Redis 管理在线状态与路由
- 稳定性：
  - WebSocket 心跳 + 断线重连 + 会话恢复（`resume_token`）

## 5. Spring 落地建议（最小可行）

模块拆分建议：
- `rtc-auth-service`
- `rtc-room-service`
- `rtc-signal-service`（WebSocket）
- `rtc-gateway`

MVP 范围：
- 1v1 + 8 人群聊
- 音频 + 摄像头视频
- 静音/开关摄像头
- 入会/退会/成员变更事件

第二阶段：
- 屏幕共享
- 云端录制
- AI 降噪 / 实时转写

---

如需，我可以继续补一版：
- OpenAPI 3.0 YAML（可直接导入 Swagger / Apifox）
- WebSocket 事件的 Java DTO（请求/响应/错误）
- Spring Boot Controller + WebSocket Handler 的最小骨架代码
