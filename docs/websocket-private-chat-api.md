# WebSocket 私聊接口文档

## 1. 目标
- 绑定登录用户到 WebSocket 会话
- 支持用户对用户私聊消息发送与接收
- 提供发送回执和业务错误通道，方便前端状态管理

## 2. 连接信息
- 握手地址: `/ws`
- 协议: STOMP over WebSocket
- 应用前缀: `/app`
- 用户目的地前缀: `/user`

## 3. 鉴权与用户绑定
- 在 STOMP `CONNECT` 帧 Header 中传入 token:
  - `Authorization: Bearer <JWT>` 或 `token: <JWT>`
- 服务端会解析 JWT，并将用户账号（`account`）绑定为当前 WebSocket `Principal.name`
- 若 token 缺失/非法/过期，连接会失败
- 订阅限制: 仅允许订阅 `/user/queue/**`

## 4. 客户端订阅通道
- 收消息: `/user/queue/messages`
- 发送回执: `/user/queue/acks`
- 业务错误: `/user/queue/errors`

## 5. 发送私聊消息
- 发送目标: `/app/chat/private.send`
- 请求体:
```json
{
  "to": "1000002",
  "content": "你好，在吗？",
  "clientMessageId": "c_17370100001"
}
```
- 字段说明:
- `to`: 接收方账号（必填）
- `content`: 文本内容（必填）
- `clientMessageId`: 客户端消息 ID（可选，建议传，用于去重和映射回执）

## 6. 下行数据格式

### 6.1 私聊消息 `/user/queue/messages`
```json
{
  "messageId": "f2f4324e-9d4a-4c58-a4df-3f908f3520d2",
  "from": "1000001",
  "to": "1000002",
  "content": "你好，在吗？",
  "clientMessageId": "c_17370100001",
  "sentAt": "2026-02-12T20:15:30.123"
}
```

### 6.2 发送回执 `/user/queue/acks`
```json
{
  "clientMessageId": "c_17370100001",
  "messageId": "f2f4324e-9d4a-4c58-a4df-3f908f3520d2",
  "to": "1000002",
  "status": "SENT",
  "ackAt": "2026-02-12T20:15:30.123"
}
```

### 6.3 业务错误 `/user/queue/errors`
```json
{
  "code": "NOT_FRIEND",
  "message": "仅支持好友之间发送私聊消息",
  "clientMessageId": "c_17370100001",
  "at": "2026-02-12T20:15:30.123"
}
```

## 7. 业务规则
- 仅好友（`ACCEPTED`）之间允许私聊
- 目标账号不存在会返回错误
- `to` 或 `content` 为空会返回错误
- 服务端会给发送方和接收方都下发消息事件，便于发送方本地会话同步
- 离线未读不再在 WebSocket CONNECT 时自动回放，改为登录后调用 `/messages/offline` 主动拉取
- 每条私聊消息会持久化到数据库，可通过 `/messages/history/{friendAccount}` 分页查询历史记录

## 8. 前端接入示例（stompjs）
```javascript
import { Client } from "@stomp/stompjs";

const token = localStorage.getItem("token");

const client = new Client({
  brokerURL: "ws://localhost:8080/ws",
  connectHeaders: {
    Authorization: `Bearer ${token}`
  },
  reconnectDelay: 5000
});

client.onConnect = () => {
  client.subscribe("/user/queue/messages", (frame) => {
    const msg = JSON.parse(frame.body);
    console.log("message", msg);
  });

  client.subscribe("/user/queue/acks", (frame) => {
    const ack = JSON.parse(frame.body);
    console.log("ack", ack);
  });

  client.subscribe("/user/queue/errors", (frame) => {
    const err = JSON.parse(frame.body);
    console.error("ws error", err);
  });
};

client.activate();

function sendPrivate(to, content, clientMessageId) {
  client.publish({
    destination: "/app/chat/private.send",
    body: JSON.stringify({ to, content, clientMessageId })
  });
}
```

## 9. 后端对应实现
- WebSocket 配置: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/config/WebSocketConfiguration.java`
- STOMP 鉴权拦截器: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/config/StompAuthChannelInterceptor.java`
- 私聊控制器: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/ChatController.java`
