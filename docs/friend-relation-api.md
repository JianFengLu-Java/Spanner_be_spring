# 好友关系 API 文档

## 概述
- Base URL: `/friends`
- 认证方式: `Authorization: Bearer <token>`
- 数据格式: `application/json`

## 关系状态说明
- `PENDING`: 待处理好友申请
- `ACCEPTED`: 已同意
- `REJECTED`: 已拒绝
- `CANCELED`: 已取消
- `EXPIRED`: 已过期
- `BLOCKED`: 拉黑状态（当前接口未提供拉黑动作）

## 1. 发送好友申请
- 方法: `POST /friends/apply`
- 请求体:
```json
{
  "friendAccount": "1000002",
  "verificationMessage": "你好，我是李四介绍的朋友"
}
```
- 成功响应:
```json
{
  "code": 200,
  "status": "success",
  "message": "好友申请已发送",
  "data": {
    "requestId": "fr_1739347200000_12ab34cd",
    "status": "PENDING",
    "operatorAccount": "1000001",
    "updatedAt": "2026-02-12T08:30:00Z",
    "expiredAt": "2026-02-19T08:30:00Z"
  }
}
```

## 2. 同意好友申请
- 方法: `POST /friends/accept`
- 请求体:
```json
{
  "friendAccount": "1000002"
}
```
- 成功响应 `data` 包含 `requestId/status/operatorAccount/updatedAt/expiredAt`。
- 成功后会自动发送一条私聊消息给申请方：
- `哈喽我们已经是好友了，快来一起聊天吧！`

## 3. 拒绝好友申请
- 方法: `POST /friends/reject`
- 请求体:
```json
{
  "friendAccount": "1000002"
}
```
- 成功响应 `data` 包含 `requestId/status/operatorAccount/updatedAt/expiredAt`。

## 4. 取消我发出的待处理申请
- 方法: `POST /friends/cancel`
- 请求体:
```json
{
  "requestId": "fr_1739347200000_12ab34cd"
}
```

## 5. 删除好友关系
- 方法: `DELETE /friends/{friendAccount}`

## 6. 查询好友列表
- 方法: `GET /friends`
- 返回的每个好友项包含 `signature`（个性签名，`String`，可为空）。
- 返回的每个好友项新增：
- `isVip`（`Boolean`）
- `growthValue`（`Long`）
- `vipLevel`（`Integer`）

## 7. 查询待处理好友申请（收到的请求）
- 方法: `GET /friends/requests/pending`
- 语义: 仅返回 `INBOUND + PENDING`
- 返回的每个申请人信息包含 `signature`（个性签名，`String`，可为空）。
- 返回的每个申请人信息新增：
- `isVip`（`Boolean`）
- `growthValue`（`Long`）
- `vipLevel`（`Integer`）

## 8. 分页查询好友申请历史
- 方法: `GET /friends/requests/history`
- Query 参数:
- `page` 默认 `1`
- `size` 默认 `20`，最大 `100`
- `direction` 可选: `INBOUND | OUTBOUND`
- `status` 可选，支持单值或逗号分隔: `PENDING,REJECTED`
- `startTime` 可选，ISO-8601 UTC
- `endTime` 可选，ISO-8601 UTC
- `keyword` 可选，按账号/昵称模糊搜索

- 成功响应:
```json
{
  "code": 200,
  "status": "success",
  "message": "查询好友申请历史成功",
  "data": {
    "records": [
      {
        "requestId": "fr_1739347200000_12ab34cd",
        "direction": "INBOUND",
        "status": "ACCEPTED",
        "applicantAccount": "1000001",
        "applicantName": "张三",
        "applicantAvatarUrl": "https://example.com/a.png",
        "applicantSignature": "保持热爱，奔赴山海",
        "targetAccount": "1000002",
        "targetName": "李四",
        "targetAvatarUrl": "https://example.com/b.png",
        "targetSignature": "今天也要开心",
        "verificationMessage": "你好，想加你为好友",
        "source": "ACCOUNT_SEARCH",
        "operatorAccount": "1000002",
        "createdAt": "2026-02-11T08:30:00Z",
        "updatedAt": "2026-02-11T09:00:00Z",
        "expiredAt": null
      }
    ],
    "page": 1,
    "size": 20,
    "total": 128,
    "totalPages": 7,
    "hasMore": true
  }
}
```

## 9. 查询单条好友申请详情
- 方法: `GET /friends/requests/history/{requestId}`
- 返回: 单条历史记录项结构，字段同 `records[]`。

## 10. 按账号查询用户
- 方法: `GET /friends/users/{account}`
- 返回字段新增 `signature`（个性签名，`String`，可为空）。
- 返回字段新增：
- `isVip`（`Boolean`）
- `growthValue`（`Long`）
- `vipLevel`（`Integer`）

## 错误码约定
- `400`: 参数错误，`errorCode=FRIEND_REQUEST_INVALID_PARAM`
- `401`: 未登录或登录状态失效，`errorCode=UNAUTHORIZED`
- `403`: 无权限，`errorCode=FRIEND_REQUEST_FORBIDDEN`
- `404`: 请求不存在，`errorCode=FRIEND_REQUEST_NOT_FOUND`
- `409`: 状态冲突，`errorCode=FRIEND_REQUEST_STATE_CONFLICT`
- `429`: 申请过于频繁，`errorCode=FRIEND_REQUEST_RATE_LIMITED`
- `500`: 服务器内部错误，`errorCode=INTERNAL_ERROR`
