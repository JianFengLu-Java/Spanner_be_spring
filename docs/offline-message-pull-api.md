# 登录后拉取离线私聊消息 API

## 1. 使用场景
- 用户登录成功后，前端主动拉取未读离线消息。
- 此接口会在返回消息后清空该用户离线队列（消费语义）。
- 聊天完整历史请使用 `/messages/history/{friendAccount}` 接口分页查询。

## 2. 接口信息
- 方法: `GET`
- 路径: `/messages/offline`
- 鉴权: `Authorization: Bearer <accessToken>`

## 3. 请求示例
```http
GET /messages/offline HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOi...
```

## 4. 成功响应
```json
{
  "code": 200,
  "status": "success",
  "message": "拉取离线消息成功",
  "data": {
    "messages": [
      {
        "messageId": "f2f4324e-9d4a-4c58-a4df-3f908f3520d2",
        "from": "1000001",
        "fromRealName": "张三",
        "fromAvatarUrl": "https://cdn.example.com/avatar-1000001.png",
        "to": "1000002",
        "content": "你好，在吗？",
        "clientMessageId": "c_17370100001",
        "sentAt": "2026-02-12T20:15:30.123"
      }
    ],
    "count": 1,
    "pulledAt": "2026-02-12T21:00:00.123"
  }
}
```

## 5. 未登录响应
```json
{
  "code": 401,
  "status": "error",
  "message": "未登录"
}
```

## 6. 前端调用建议
- 登录成功拿到 token 后立即调用一次 `/messages/offline`。
- 拉取完成后再进入会话页渲染，避免用户错过历史未读。
- 与 WebSocket 实时消息合并时，按 `messageId` 去重。

## 7. 前端示例（Axios）
```javascript
import axios from "axios";

async function pullOfflineMessages(accessToken) {
  const res = await axios.get("http://localhost:8080/messages/offline", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
  return res.data?.data?.messages ?? [];
}
```
