# Token 鉴权与刷新接口文档

## 概述
- 用户接口前缀: `/user`
- 鉴权方式: `Authorization: Bearer <accessToken>`
- Token 模型:
- `accessToken`: 15 分钟有效（900 秒）
- `refreshToken`: 7 天有效
- 刷新策略: 每次刷新都会返回新的 `accessToken` 和新的 `refreshToken`（轮换）

## 1. 登录
- 方法: `POST /user/login`
- 请求体:
```json
{
  "account": "1000001",
  "password": "123456"
}
```

### 1.1 登录成功
- HTTP 状态码: `200`
- 响应体:
```json
{
  "code": 200,
  "message": "登录成功！",
  "token": "eyJhbGciOiJIUzI1NiJ9....",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9....",
  "accessTokenExpiresIn": 900,
  "data": {
    "account": "1000001",
    "realName": "张三",
    "avatarUrl": "https://example.com/a.png",
    "gender": "M",
    "email": "zhangsan@example.com",
    "phone": "13800000000",
    "address": "Shanghai",
    "signature": "保持热爱，奔赴山海",
    "age": 20
  }
}
```

### 1.2 登录失败
- HTTP 状态码: `401`
- 可能响应体:
```json
{
  "code": 401,
  "message": "用户不存在",
  "token": null,
  "refreshToken": null,
  "accessTokenExpiresIn": null,
  "data": null
}
```
```json
{
  "code": 403,
  "message": "密码错误",
  "token": null,
  "refreshToken": null,
  "accessTokenExpiresIn": null,
  "data": null
}
```

## 2. 刷新 Token
- 方法: `POST /user/refresh`
- 请求体:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...."
}
```

### 2.1 刷新成功
- HTTP 状态码: `200`
- 响应体:
```json
{
  "code": 200,
  "message": "刷新成功",
  "token": "new_access_token",
  "refreshToken": "new_refresh_token",
  "accessTokenExpiresIn": 900,
  "data": null
}
```

### 2.2 刷新失败
- HTTP 状态码: `401`
- 可能响应体:
```json
{
  "code": 401,
  "message": "缺少 refreshToken",
  "token": null,
  "refreshToken": null,
  "accessTokenExpiresIn": null,
  "data": null
}
```
```json
{
  "code": 401,
  "message": "refreshToken 无效或已过期",
  "token": null,
  "refreshToken": null,
  "accessTokenExpiresIn": null,
  "data": null
}
```

## 3. 受保护接口调用
- 除 `/user/login`、`/user/register`、`/user/refresh` 之外，其他接口默认需要携带 `accessToken`。
- 请求头示例:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9....
```
- 当 `accessToken` 过期时，接口会返回 `401`。

## 4. 获取当前用户信息
- 方法: `GET /user/me`
- 请求头:
```http
Authorization: Bearer <accessToken>
```
- HTTP 状态码: `200`
- 响应体:
```json
{
  "user": {
    "account": "1000001",
    "realName": "张三",
    "avatarUrl": "https://example.com/a.png",
    "gender": "M",
    "email": "zhangsan@example.com",
    "phone": "13800000000",
    "address": "Shanghai",
    "signature": "保持热爱，奔赴山海",
    "age": 20
  }
}
```

## 5. 修改当前用户信息
- 方法: `PUT /user/me`
- 请求头:
```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```
- 请求体（全部字段可选，传了才会更新）:
```json
{
  "realName": "张三",
  "avatarUrl": "https://example.com/new-avatar.png",
  "gender": "M",
  "email": "zhangsan@example.com",
  "phone": "13800000000",
  "address": "Shanghai",
  "signature": "新的个性签名",
  "age": 21
}
```
- 字段说明:
- `signature`: 个性签名，类型 `String`，可为空；老数据未设置时返回 `null`，兼容历史数据。
- HTTP 状态码: `200`
- 响应体:
```json
{
  "user": {
    "account": "1000001",
    "realName": "张三",
    "avatarUrl": "https://example.com/new-avatar.png",
    "gender": "M",
    "email": "zhangsan@example.com",
    "phone": "13800000000",
    "address": "Shanghai",
    "signature": "新的个性签名",
    "age": 21
  }
}
```

## 6. 前端推荐调用流程
1. 登录成功后，保存 `token` 与 `refreshToken`。
2. 普通请求统一带上 `Authorization: Bearer <token>`。
3. 请求返回 `401` 时，调用 `/user/refresh` 换新 token。
4. 刷新成功后，覆盖本地 token 并重放原请求。
5. 刷新失败（401）时，清理本地登录态并跳转登录页。

## 7. 前端伪代码示例
```javascript
async function requestWithAuth(config) {
  let accessToken = localStorage.getItem("token");
  const refreshToken = localStorage.getItem("refreshToken");

  try {
    return await request({
      ...config,
      headers: { ...config.headers, Authorization: `Bearer ${accessToken}` }
    });
  } catch (err) {
    if (err?.response?.status !== 401) throw err;

    const refreshRes = await request({
      url: "/user/refresh",
      method: "POST",
      data: { refreshToken }
    });

    accessToken = refreshRes.data.token;
    localStorage.setItem("token", refreshRes.data.token);
    localStorage.setItem("refreshToken", refreshRes.data.refreshToken);

    return await request({
      ...config,
      headers: { ...config.headers, Authorization: `Bearer ${accessToken}` }
    });
  }
}
```

## 8. 后端实现位置
- 控制器: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/UserController.java`
- 服务: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/impl/UserServiceImpl.java`
- JWT 工具: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/util/JwtUtil.java`
- 安全配置: `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/config/SecurityConfiguration.java`
