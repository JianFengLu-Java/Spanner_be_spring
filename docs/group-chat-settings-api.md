# 群聊设置详情页 API 文档

## 1. 统一约定
- 鉴权：`Authorization: Bearer <accessToken>`
- 统一响应：`{ code, status, message, data }`
- 角色：`OWNER | ADMIN | MEMBER`
- 时间字段：`LocalDateTime`（服务端返回 ISO 风格字符串）

## 2. P0 接口

### 2.0 获取群资料
- `GET /groups/{groupNo}/profile`
- 返回：`GroupProfile`

### 2.01 编辑群资料
- `PUT /groups/{groupNo}/profile`
- 请求体（Patch 语义）： 
```json
{
  "groupName": "项目协作群-后端",
  "groupAvatarUrl": "https://cdn.example.com/group/20010001.png",
  "summary": "项目沟通与排期同步"
}
```
- 权限规则：
- `groupName`：`OWNER | ADMIN`；当 `memberCanEditGroupName=true` 时 `MEMBER` 也可改
- `groupAvatarUrl/summary`：仅 `OWNER | ADMIN`

### 2.1 获取群设置详情
- `GET /groups/{groupNo}/settings/detail`
- 返回：
```json
{
  "groupProfile": {},
  "mySettings": {},
  "latestAnnouncement": {},
  "mediaOverview": {}
}
```

### 2.2 获取群成员（分页/筛选）
- `GET /groups/{groupNo}/members?page=1&size=50&keyword=张三&role=ADMIN`
- Query：
- `page` 默认 `1`
- `size` 默认 `50`，最大 `200`
- `keyword` 按账号/昵称模糊匹配
- `role` 可选：`OWNER | ADMIN | MEMBER`
- 返回分页字段：`records/page/size/total/totalPages/hasMore`

### 2.3 更新我的群设置
- `PUT /groups/{groupNo}/settings/my`
- 请求体（Patch 语义，支持部分字段）：
```json
{
  "messageMute": true,
  "chatPinned": false,
  "saveToContacts": true
}
```

### 2.4 更新群权限配置
- `PUT /groups/{groupNo}/settings/permissions`
- 权限：`OWNER | ADMIN`
- 请求体：
```json
{
  "inviteMode": "ADMIN_ONLY",
  "memberCanEditGroupName": false,
  "joinVerificationEnabled": true,
  "announcementPermission": "OWNER_ADMIN"
}
```

### 2.5 获取最新群公告
- `GET /groups/{groupNo}/announcements/latest`
- 返回：`GroupAnnouncement | null`

### 2.6 发布群公告
- `POST /groups/{groupNo}/announcements`
- 请求体：
```json
{
  "content": "本周五发布版本，今晚 9 点冻结代码。"
}
```

### 2.7 编辑群公告
- `PUT /groups/{groupNo}/announcements/{announcementId}`
- 请求体：
```json
{
  "content": "更新后的公告内容"
}
```

### 2.8 退出群聊
- `POST /groups/{groupNo}/quit`
- 规则：群主退出返回冲突（需先转让）
- 返回：
```json
{
  "groupNo": "20010001",
  "quit": true,
  "shouldRemoveLocalSession": true
}
```

### 2.9 解散群聊
- `DELETE /groups/{groupNo}`
- 权限：仅 `OWNER`

### 2.10 举报群聊
- `POST /groups/{groupNo}/reports`
- 请求体：
```json
{
  "reasonType": "SPAM",
  "description": "疑似广告刷屏",
  "evidenceUrls": ["https://cdn.example.com/report/xxx.png"]
}
```
- 返回：
```json
{
  "reportNo": "gr_17399500001234"
}
```

## 3. P1 接口

### 3.1 群成员预览
- `GET /groups/{groupNo}/members/preview?size=9`
- `size` 默认 `9`，最大 `18`

### 3.2 批量邀请成员
- `POST /groups/{groupNo}/members/batch-invite`
- 请求体：
```json
{
  "accounts": ["10008", "10009"],
  "source": "GROUP_SETTINGS"
}
```
- 返回：
```json
{
  "successAccounts": ["10008"],
  "failed": [
    {
      "account": "10009",
      "reasonCode": "ALREADY_IN_GROUP",
      "reason": "用户已在群内"
    }
  ]
}
```

### 3.3 移除成员
- `POST /groups/{groupNo}/members/{account}/remove`
- 权限：`OWNER | ADMIN`

### 3.4 更新成员角色
- `PUT /groups/{groupNo}/members/{account}/role`
- 权限：仅 `OWNER`
- 请求体：
```json
{
  "role": "ADMIN"
}
```

### 3.5 公告历史
- `GET /groups/{groupNo}/announcements?page=1&size=20`

### 3.6 媒体统计
- `GET /groups/{groupNo}/media/overview`

### 3.7 清空消息（可选跨端语义）
- `POST /groups/{groupNo}/messages/clear`
- 请求体（可选）：
```json
{
  "scope": "SELF"
}
```

## 4. 兼容接口（保留）
- `PUT /groups/{groupNo}/announcement`
- `PUT /groups/{groupNo}/admins`
- `DELETE /groups/{groupNo}/admins/{account}`
- `DELETE /groups/{groupNo}/members/{account}`
- `POST /groups/{groupNo}/kick`

## 5. 错误码约定
- `400` 参数错误
- `401` 未登录
- `403` 无权限
- `404` 群/成员/公告不存在
- `409` 业务冲突（群满、群主退群等）
- `500` 服务异常

### 2.02 查询我加入的群
- `GET /groups/my?page=1&size=20&keyword=项目`
- 返回分页 `GroupProfile` 列表。
