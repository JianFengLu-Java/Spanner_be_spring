# 用户钱包 API 文档

## 1. 基础说明
- 接口前缀：`/user`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 返回结构：沿用现有项目 `code/status/data` 风格

## 2. 查询当前钱包
- 方法：`GET /user/wallet`
- 请求头：
```http
Authorization: Bearer <accessToken>
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "walletNo": "W1000001",
    "balance": 100.00,
    "currency": "CNY",
    "status": "ACTIVE",
    "updatedAt": "2026-02-15T16:20:30"
  }
}
```

## 3. 钱包充值
- 方法：`POST /user/wallet/recharge`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体：
```json
{
  "amount": 50.00,
  "businessNo": "ORDER_202602150001",
  "remark": "手动充值"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "changeType": "RECHARGE",
    "businessNo": "ORDER_202602150001",
    "remark": "手动充值",
    "amount": 50.00,
    "beforeBalance": 100.00,
    "afterBalance": 150.00,
    "changeTime": "2026-02-15T16:21:10",
    "wallet": {
      "walletNo": "W1000001",
      "balance": 150.00,
      "currency": "CNY",
      "status": "ACTIVE",
      "updatedAt": "2026-02-15T16:21:10"
    }
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "amount 必须大于 0"
}
```

## 4. 钱包消费
- 方法：`POST /user/wallet/consume`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体：
```json
{
  "amount": 30.00,
  "businessNo": "PAY_202602150001",
  "remark": "购买商品"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "changeType": "CONSUME",
    "businessNo": "PAY_202602150001",
    "remark": "购买商品",
    "amount": 30.00,
    "beforeBalance": 150.00,
    "afterBalance": 120.00,
    "changeTime": "2026-02-15T16:22:00",
    "wallet": {
      "walletNo": "W1000001",
      "balance": 120.00,
      "currency": "CNY",
      "status": "ACTIVE",
      "updatedAt": "2026-02-15T16:22:00"
    }
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "钱包余额不足"
}
```

## 5. 字段说明
- `amount`: 本次变更金额，必须大于 0。
- `businessNo`: 业务单号，可选；为空时后端自动生成。
- `remark`: 备注信息，可选。
- `changeType`: `RECHARGE` 或 `CONSUME`。

## 6. 查询钱包流水
- 方法：`GET /user/wallet/flows`
- 请求头：
```http
Authorization: Bearer <accessToken>
```
- Query 参数：
- `page`：页码，从 1 开始，默认 `1`
- `size`：每页数量，范围 `1-100`，默认 `20`
- `changeType`：可选，`RECHARGE` 或 `CONSUME`

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "data": {
    "records": [
      {
        "walletNo": "W1000001",
        "businessNo": "PAY_202602150001",
        "changeType": "CONSUME",
        "amount": 30.00,
        "beforeBalance": 150.00,
        "afterBalance": 120.00,
        "remark": "购买商品",
        "createdAt": "2026-02-15T16:22:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 10,
    "totalPages": 1,
    "hasMore": false
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "changeType 仅支持 RECHARGE/CONSUME"
}
```

## 7. 前端调用建议
1. 页面初始化时调用 `GET /user/wallet` 展示余额。
2. 充值/消费成功后，直接使用响应中的 `wallet.balance` 刷新界面。
3. 交易记录页调用 `GET /user/wallet/flows` 分页拉取流水。
4. 对 `400` 错误弹出后端 `message`。
5. 对 `401` 继续沿用当前 token 刷新机制。
