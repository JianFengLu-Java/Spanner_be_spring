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
    "securityPasswordSet": true,
    "updatedAt": "2026-02-15T16:20:30"
  }
}
```

## 3. 设置/修改钱包安全密码
- 方法：`PUT /user/wallet/security-password`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体（首次设置时可不传 `oldSecurityPassword`）：
```json
{
  "oldSecurityPassword": "123456",
  "newSecurityPassword": "654321"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "message": "钱包安全密码设置成功",
  "data": {
    "walletNo": "W1000001",
    "balance": 100.00,
    "currency": "CNY",
    "status": "ACTIVE",
    "securityPasswordSet": true,
    "updatedAt": "2026-02-15T16:21:00"
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "原安全密码错误"
}
```

## 4. 钱包充值
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

## 5. 钱包消费
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
  "remark": "购买商品",
  "securityPassword": "654321"
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
  "message": "钱包安全密码错误"
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

## 6. 字段说明
- `amount`: 本次变更金额，必须大于 0。
- `businessNo`: 业务单号，可选；为空时后端自动生成。
- `remark`: 备注信息，可选。
- `securityPassword`: 钱包消费和转账时必填，为已设置的 6 位数字安全密码。
- `changeType`: `RECHARGE`、`CONSUME`、`TRANSFER_OUT`、`TRANSFER_IN`。
- `securityPasswordSet`: 钱包是否已设置安全密码。

## 7. 发起转账申请（待确认）
- 方法：`POST /user/wallet/transfer`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体：
```json
{
  "toAccount": "1000002",
  "amount": 20.00,
  "securityPassword": "654321",
  "businessNo": "TRF_202602150001",
  "remark": "AA收款"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "message": "转账申请已创建，等待收款方确认",
  "data": {
    "businessNo": "TRF_202602150001",
    "toAccount": "1000002",
    "amount": 20.00,
    "remark": "AA收款",
    "status": "PENDING",
    "createdAt": "2026-02-15T16:25:00"
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "收款账号不存在"
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "钱包安全密码错误"
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "businessNo 已存在"
}
```

> 说明：该接口仅创建待确认转账单，不会立即变更双方钱包余额，也不会写 `TRANSFER_IN/TRANSFER_OUT` 流水。

## 8. 确认转账并入账
- 方法：`POST /user/wallet/transfer/accept`
- 请求头：
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体：
```json
{
  "businessNo": "TRF_202602150001"
}
```

### 成功响应（200）
```json
{
  "code": 200,
  "status": "success",
  "message": "转账已确认并入账",
  "data": {
    "businessNo": "TRF_202602150001",
    "toAccount": "1000002",
    "amount": 20.00,
    "remark": "AA收款",
    "fromBeforeBalance": 120.00,
    "fromAfterBalance": 100.00,
    "toBeforeBalance": 5.00,
    "toAfterBalance": 25.00,
    "changeTime": "2026-02-15T16:25:30",
    "fromWallet": {
      "walletNo": "W1000001",
      "balance": 100.00,
      "currency": "CNY",
      "status": "ACTIVE",
      "securityPasswordSet": true,
      "updatedAt": "2026-02-15T16:25:30"
    },
    "toWallet": {
      "walletNo": "W1000002",
      "balance": 25.00,
      "currency": "CNY",
      "status": "ACTIVE",
      "securityPasswordSet": false,
      "updatedAt": "2026-02-15T16:25:30"
    }
  }
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "仅收款方可确认该转账"
}
```

### 失败响应（400）
```json
{
  "code": 400,
  "status": "fail",
  "message": "付款方钱包余额不足"
}
```

## 9. 查询钱包流水
- 方法：`GET /user/wallet/flows`
- 请求头：
```http
Authorization: Bearer <accessToken>
```
- Query 参数：
- `page`：页码，从 1 开始，默认 `1`
- `size`：每页数量，范围 `1-100`，默认 `20`
- `changeType`：可选，`RECHARGE`、`CONSUME`、`TRANSFER_OUT`、`TRANSFER_IN`

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
  "message": "changeType 仅支持 RECHARGE/CONSUME/TRANSFER_OUT/TRANSFER_IN"
}
```

## 10. 前端调用建议
1. 页面初始化时调用 `GET /user/wallet` 展示余额。
2. 当 `securityPasswordSet=false` 时，引导用户先调用 `PUT /user/wallet/security-password` 设置安全密码。
3. 发起转账时调用 `POST /user/wallet/transfer`，仅创建待确认单，不刷新余额。
4. 收款方点击聊天“接受”时调用 `POST /user/wallet/transfer/accept`，成功后再刷新双方余额。
5. 消费和发起转账前弹出安全密码输入框，并传入 `securityPassword`。
6. 充值/消费成功后，直接使用响应中的 `wallet.balance` 刷新界面。
7. 交易记录页调用 `GET /user/wallet/flows` 分页拉取流水。
8. 对 `400` 错误弹出后端 `message`。
9. 对 `401` 继续沿用当前 token 刷新机制。
