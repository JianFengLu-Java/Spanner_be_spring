# 群聊管理与历史消息 API

## 1. 目标
- 提供群组创建、入群、退群、成员管理、公告管理能力。
- 支持邀请好友入群、踢出成员。
- 每个群有唯一群号，成员上限 500。
- 提供群聊历史消息分页查询。

## 2. 鉴权
- 所有接口都需要：`Authorization: Bearer <accessToken>`

## 3. 数据规则
- 群号 `groupNo`：服务端生成，唯一。
- 群人数上限：`500`。
- 角色：`OWNER`（群主）、`ADMIN`（管理员）、`MEMBER`（普通成员）。

## 4. 接口列表

### 4.1 创建群组
- 方法：`POST`
- 路径：`/groups`
- 请求体：
```json
{
  "groupName": "技术交流群",
  "announcement": "欢迎入群，禁止广告"
}
```
- 成功响应：
```json
{
  "code": 200,
  "status": "success",
  "message": "创建群组成功",
  "data": {
    "groupNo": "735662840120",
    "groupName": "技术交流群",
    "ownerAccount": "1000001",
    "announcement": "欢迎入群，禁止广告",
    "maxMembers": 500,
    "memberCount": 1,
    "myRole": "OWNER",
    "createdAt": "2026-02-17T10:00:00",
    "updatedAt": "2026-02-17T10:00:00"
  }
}
```

### 4.2 查询群信息
- 方法：`GET`
- 路径：`/groups/{groupNo}`
- 说明：仅群成员可查

### 4.3 查询群成员
- 方法：`GET`
- 路径：`/groups/{groupNo}/members`
- 说明：仅群成员可查

### 4.4 加入群组
- 方法：`POST`
- 路径：`/groups/{groupNo}/join`
- 说明：重复加入幂等返回；满员返回冲突

### 4.5 退出群组
- 方法：`POST`
- 路径：`/groups/{groupNo}/quit`
- 说明：群主暂不支持退群（需先转让）

### 4.6 邀请好友入群
- 方法：`POST`
- 路径：`/groups/{groupNo}/invite`
- 请求体：
```json
{
  "friendAccount": "1000002"
}
```
- 规则：
- 邀请人必须在群内
- 仅可邀请自己的好友（已通过好友关系）
- 重复邀请幂等
- 群满 500 人返回冲突

### 4.7 更新群公告
- 方法：`PUT`
- 路径：`/groups/{groupNo}/announcement`
- 请求体：
```json
{
  "announcement": "本周六晚 8 点语音会议"
}
```
- 权限：`OWNER`、`ADMIN`

### 4.8 设置管理员
- 方法：`PUT`
- 路径：`/groups/{groupNo}/admins`
- 请求体：
```json
{
  "account": "1000002"
}
```
- 权限：仅 `OWNER`

### 4.9 取消管理员
- 方法：`DELETE`
- 路径：`/groups/{groupNo}/admins/{account}`
- 权限：仅 `OWNER`

### 4.10 踢出成员
- 方法：`DELETE`
- 路径：`/groups/{groupNo}/members/{account}`
- 权限：`OWNER`、`ADMIN`
- 规则：
- 不能踢群主
- 管理员不能互踢
- 不能踢自己

### 4.11 踢出成员（别名接口）
- 方法：`POST`
- 路径：`/groups/{groupNo}/kick`
- 请求体：
```json
{
  "account": "1000003"
}
```
- 说明：与 `DELETE /groups/{groupNo}/members/{account}` 等价

### 4.12 查询群聊历史
- 方法：`GET`
- 路径：`/groups/{groupNo}/messages/history?page=1&size=20`
- 说明：
- 按时间正序返回（旧 -> 新）
- `size` 最大 100
- 成功响应：
```json
{
  "code": 200,
  "status": "success",
  "message": "查询群聊记录成功",
  "data": {
    "messages": [
      {
        "messageId": "8f536ea3-9201-418a-a622-f893d4878481",
        "groupNo": "735662840120",
        "from": "1000001",
        "fromRealName": "张三",
        "fromAvatarUrl": "https://cdn.example.com/avatar-1000001.png",
        "content": "大家好",
        "clientMessageId": "g_17370100001",
        "sentAt": "2026-02-17T10:15:30.123"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 100,
    "totalPages": 5,
    "hasMore": true
  }
}
```

## 5. 常见失败响应
- 未登录：`401`
- 群组不存在：`404`
- 无权限或不在群内：`403`
- 参数错误：`400`
- 业务冲突（如群已满）：`409`
