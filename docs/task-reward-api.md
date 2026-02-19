# 任务奖励 API 文档

## 1. POST /task-events
接收业务事件并触发任务奖励。

### 请求体
```json
{
  "eventId": "8c49587b-4d2c-42e9-b827-4d6b7218c0d8",
  "eventType": "POST_CREATE",
  "bizId": "moment_10001",
  "actorUserId": 9527,
  "targetId": null,
  "createdAt": "2026-02-19T10:30:00Z",
  "meta": {
    "isDraft": false,
    "isDeleted": false,
    "isBlocked": false,
    "isSystemBackfill": false,
    "contentLength": 120,
    "ip": "1.2.3.4",
    "deviceId": "ios_abc"
  }
}
```

### 成功响应
```json
{
  "code": 200,
  "status": "success",
  "message": "处理成功",
  "data": {
    "eventId": "8c49587b-4d2c-42e9-b827-4d6b7218c0d8",
    "taskType": "POST_CREATE",
    "grantStatus": "GRANTED",
    "rewardWalletCent": 500,
    "rewardGrowth": 0,
    "todayGrantedCount": 1,
    "todayRemainingCount": 2,
    "reason": "OK",
    "duplicate": false
  }
}
```

### 重复事件响应
HTTP 409，`data.duplicate=true`，返回首次处理结果。

---

## 2. GET /tasks/config
查询任务配置。

### 响应
```json
{
  "code": 200,
  "status": "success",
  "message": "查询成功",
  "data": [
    {
      "taskType": "POST_CREATE",
      "enabled": true,
      "rewardWalletCent": 500,
      "rewardGrowth": 0,
      "dailyLimit": 3
    },
    {
      "taskType": "REPLY_CREATE",
      "enabled": true,
      "rewardWalletCent": 0,
      "rewardGrowth": 15,
      "dailyLimit": null
    }
  ]
}
```

---

## 3. GET /users/{id}/rewards/today
查询用户今日发帖奖励进度。

### 响应
```json
{
  "code": 200,
  "status": "success",
  "message": "查询成功",
  "data": {
    "userId": 9527,
    "date": "2026-02-19",
    "timezone": "Asia/Shanghai",
    "postGrantedCount": 2,
    "postDailyLimit": 3,
    "postRemainingCount": 1
  }
}
```

---

## 4. GET /users/{id}/wallet/ledger?page=0&size=20
查询余额奖励流水。

### 响应
```json
{
  "code": 200,
  "status": "success",
  "message": "查询成功",
  "data": {
    "account": {
      "userId": 9527,
      "balanceCent": 10500,
      "version": 11
    },
    "items": [
      {
        "ledgerId": "wl_8c49587b-4d2c-42e9-b827-4d6b7218c0d8",
        "bizType": "TASK_REWARD",
        "taskType": "POST_CREATE",
        "bizId": "moment_10001",
        "changeCent": 500,
        "balanceAfterCent": 10500,
        "createdAt": "2026-02-19T10:30:01"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  }
}
```

---

## 5. GET /users/{id}/growth/ledger?page=0&size=20
查询成长值奖励流水。

### 响应
```json
{
  "code": 200,
  "status": "success",
  "message": "查询成功",
  "data": {
    "account": {
      "userId": 9527,
      "growthValue": 345,
      "version": 7
    },
    "items": [
      {
        "ledgerId": "gl_7d9553be-f3f2-4fcb-ab17-97a560265c8a",
        "bizType": "TASK_REWARD",
        "taskType": "REPLY_CREATE",
        "bizId": "comment_9001",
        "changeGrowth": 15,
        "growthAfter": 345,
        "createdAt": "2026-02-19T11:00:01"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1
  }
}
```

---

## 错误码
- `INVALID_EVENT` 参数不合法
- `TASK_DISABLED` 任务关闭
- `EVENT_INVALID_STATE` 事件不满足有效条件
- `DAILY_LIMIT_REACHED` 发帖奖励超每日上限
- `RISK_REPLY_TOO_FREQUENT` 回复过于频繁
- `RISK_REPLY_RATE_LIMIT` 单帖分钟回复奖励超阈值
- `INTERNAL_ERROR` 服务异常
