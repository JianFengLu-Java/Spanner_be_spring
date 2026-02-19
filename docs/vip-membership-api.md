# 用户 VIP 会员 API 文档

## 1. 基础说明
- 接口前缀：`/user`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 返回结构：`code/status/data`，失败时含 `message`

## 2. 会员套餐列表
- 方法：`GET /user/vip/plans`

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": [
    {
      "planCode": "MONTHLY",
      "planName": "月费会员",
      "price": 20.00,
      "months": 1,
      "growthBonus": 100
    },
    {
      "planCode": "QUARTERLY",
      "planName": "季度会员",
      "price": 58.00,
      "months": 3,
      "growthBonus": 350
    },
    {
      "planCode": "YEARLY",
      "planName": "年费会员",
      "price": 218.00,
      "months": 12,
      "growthBonus": 1500
    }
  ]
}
```

### 开通成功后的系统通知
- 会员开通成功后，服务端会自动发送一条系统私信给当前用户：
  - 发送方账号：`SYSTEM`
  - 接收方账号：当前登录用户账号
  - 通知内容包含：套餐名称、成长值到账、当前等级、会员到期时间
- 推送策略：
  - 在线：通过 WebSocket 实时下发到 `/user/queue/messages`
  - 离线：写入离线消息队列，用户上线后通过离线消息拉取接口获取

## 3. 查询我的会员资料
- 方法：`GET /user/vip/profile`

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "vipActive": true,
    "vipExpireAt": "2026-05-20T10:30:00",
    "growthValue": 420,
    "userLevel": 3,
    "nextLevelGrowth": 700
  }
}
```

## 4. 开通/续费会员
- 方法：`POST /user/vip/purchase`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体：
```json
{
  "planCode": "MONTHLY",
  "securityPassword": "123456",
  "purchaseNo": "VIP_202602190001"
}
```

### 字段说明
- `planCode`：可选，支持 `MONTHLY/QUARTERLY/YEARLY`，不传默认 `MONTHLY`（20 元/月）。
- `securityPassword`：必填，钱包 6 位安全密码。
- `purchaseNo`：可选，自定义业务单号；不传后端自动生成。

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "message": "会员开通成功",
  "data": {
    "purchaseNo": "VIP_202602190001",
    "planCode": "MONTHLY",
    "planName": "月费会员",
    "amount": 20.00,
    "startAt": "2026-02-19T10:30:00",
    "endAt": "2026-03-19T10:30:00",
    "vipActive": true,
    "vipExpireAt": "2026-03-19T10:30:00",
    "growthValue": 520,
    "userLevel": 3
  }
}
```

### 失败响应（400）示例
```json
{
  "code": 400,
  "status": "fail",
  "message": "钱包余额不足"
}
```
```json
{
  "code": 400,
  "status": "fail",
  "message": "钱包安全密码错误"
}
```
```json
{
  "code": 400,
  "status": "fail",
  "message": "purchaseNo 已存在"
}
```

## 5. 手动增加成长值（测试/运营场景）
- 方法：`POST /user/growth/add`
- 请求体：
```json
{
  "growthValue": 50,
  "reason": "运营活动奖励"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "vipActive": true,
    "vipExpireAt": "2026-03-19T10:30:00",
    "growthValue": 570,
    "userLevel": 3,
    "nextLevelGrowth": 700
  }
}
```

### 失败响应（400）示例
```json
{
  "code": 400,
  "status": "fail",
  "message": "growthValue 必须大于 0"
}
```

## 6. 会员订单分页
- 方法：`GET /user/vip/orders?page=1&size=20`

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "records": [
      {
        "purchaseNo": "VIP_202602190001",
        "planCode": "MONTHLY",
        "planName": "月费会员",
        "amount": 20.00,
        "months": 1,
        "growthBonus": 100,
        "startAt": "2026-02-19T10:30:00",
        "endAt": "2026-03-19T10:30:00",
        "status": "SUCCESS",
        "createdAt": "2026-02-19T10:30:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "hasMore": false
  }
}
```

## 7. 相关接口字段变化
- `GET /user/me` 返回的 `user` 已新增字段：
  - `vipExpireAt`
  - `growthValue`
  - `userLevel`

## 8. 钱包流水类型补充
- 会员购买会写钱包流水 `changeType = VIP_PURCHASE`。
- `GET /user/wallet/flows` 的 `changeType` 过滤新增支持：`VIP_PURCHASE`。
