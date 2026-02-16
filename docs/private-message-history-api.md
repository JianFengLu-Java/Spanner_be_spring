# 私聊历史消息查询 API

## 1. 目标
- 提供登录用户与某个好友的私聊历史记录分页查询。
- 消息按时间正序返回（旧 -> 新），便于前端直接渲染会话。

## 2. 接口信息
- 方法: `GET`
- 路径: `/messages/history/{friendAccount}`
- 鉴权: `Authorization: Bearer <accessToken>`

## 3. 请求参数
- Path 参数:
- `friendAccount`: 好友账号（必填）

- Query 参数:
- `page`: 页码，从 `1` 开始，默认 `1`
- `size`: 每页条数，默认 `20`，最大 `100`

## 4. 请求示例
```http
GET /messages/history/1000002?page=1&size=20 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOi...
```

## 5. 成功响应
```json
{
  "code": 200,
  "status": "success",
  "message": "查询聊天记录成功",
  "data": {
    "messages": [
      {
        "messageId": "f2f4324e-9d4a-4c58-a4df-3f908f3520d2",
        "from": "1000001",
        "to": "1000002",
        "content": "你好，在吗？",
        "clientMessageId": "c_17370100001",
        "sentAt": "2026-02-12T20:15:30.123"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 135,
    "totalPages": 7,
    "hasMore": true
  }
}
```

## 6. 失败响应
### 6.1 未登录
```json
{
  "code": 401,
  "status": "error",
  "message": "未登录"
}
```

### 6.2 目标用户不存在
```json
{
  "code": 404,
  "status": "error",
  "message": "目标用户不存在"
}
```

### 6.3 非好友关系
```json
{
  "code": 403,
  "status": "error",
  "message": "仅支持查询好友之间的聊天记录"
}
```

## 7. 前端调用建议
- 首次进入会话页: 拉取 `page=1`。
- 上拉加载更多: `page` 递增，直到 `hasMore=false`。
- 与 WebSocket 实时消息合并时，按 `messageId` 去重。
