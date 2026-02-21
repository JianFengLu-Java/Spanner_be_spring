# 动态模块 API 文档

## 概述
- Base URL: `/moments`
- 鉴权方式: `Authorization: Bearer <token>`
- 数据格式: `application/json`
- 统一响应结构:

```json
{
  "code": 200,
  "status": "success",
  "message": "success",
  "data": {}
}
```

## 数据模型

### MomentItem
```json
{
  "id": "m_1739358000000_ab12cd34",
  "title": "今日份打卡",
  "cover": "https://cdn.example.com/moments/cover-1.jpg",
  "author": {
    "account": "10001",
    "name": "张三",
    "avatar": "https://cdn.example.com/avatar-10001.png"
  },
  "content": "纯文本内容",
  "contentHtml": "<p>富文本内容</p>",
  "images": ["https://cdn.example.com/moments/img-1.jpg"],
  "likes": 12,
  "isLiked": false,
  "likePreviewUsers": [
    {
      "account": "10002",
      "name": "李四",
      "avatar": "https://cdn.example.com/avatar-10002.png"
    }
  ],
  "commentsCount": 3,
  "isFavorited": false,
  "friendStatusWithAuthor": "NONE",
  "timestamp": "2026-02-12T08:30:00Z",
  "createdAt": "2026-02-12T08:30:00Z",
  "updatedAt": "2026-02-12T08:30:00Z"
}
```

### CursorPage<T>
```json
{
  "records": [],
  "nextCursor": "2026-02-12T08:00:00Z_m_1739358000000_ab12cd34",
  "hasMore": true
}
```

### MomentCommentItem
```json
{
  "id": "mc_1739359000000_ef56gh78",
  "momentId": "m_1739358000000_ab12cd34",
  "parentCommentId": null,
  "replyToAccount": null,
  "author": {
    "account": "10002",
    "name": "李四",
    "avatar": "https://cdn.example.com/avatar-10002.png"
  },
  "text": "评论内容",
  "likes": 0,
  "isLiked": false,
  "replyCount": 0,
  "timestamp": "2026-02-12T09:00:00Z",
  "createdAt": "2026-02-12T09:00:00Z"
}
```

### MomentLikeUser
```json
{
  "account": "10003",
  "name": "王五",
  "avatar": "https://cdn.example.com/avatar-10003.png",
  "likedAt": "2026-02-12T09:30:00Z"
}
```

### MomentAboutMeItem
```json
{
  "id": "mc_1739359000000_ef56gh78",
  "type": "COMMENT_ON_MY_MOMENT",
  "momentId": "m_1739358000000_ab12cd34",
  "momentTitle": "今日份打卡",
  "sourceCommentId": "mc_1739359000000_ef56gh78",
  "parentCommentId": null,
  "fromUser": {
    "account": "10002",
    "name": "李四",
    "avatar": "https://cdn.example.com/avatar-10002.png"
  },
  "content": "评论内容",
  "targetContent": "我的动态正文摘要",
  "timestamp": "2026-02-12T09:00:00Z",
  "createdAt": "2026-02-12T09:00:00Z"
}
```

## 接口列表

## 1. 获取动态流
- 方法: `GET /moments`
- Query 参数:
- `tab`: `recommend | friends | nearby | trending`，默认 `recommend`
- `keyword`: 可选，标题/内容/作者名模糊搜索
- `cursor`: 可选，游标
- `size`: 可选，默认 `20`，范围 `1-50`
- `lat`/`lng`: 可选（当前版本预留）

- 成功响应: `data` 为 `CursorPage<MomentItem>`

## 2. 获取动态详情
- 方法: `GET /moments/{momentId}`
- 成功响应: `data` 为 `MomentItem`

## 2.1 获取关于我的动态
- 方法: `GET /moments/about-me`
- Query 参数:
- `cursor`: 可选，游标
- `size`: 可选，默认 `20`，范围 `1-50`

- 成功响应: `data` 为 `CursorPage<MomentAboutMeItem>`
- `type` 枚举:
- `COMMENT_ON_MY_MOMENT`: 别人评论了我的动态（一级评论）
- `REPLY_TO_ME`: 别人回复了我的评论（二级回复）

## 3. 发布动态
- 方法: `POST /moments`
- 请求体:
```json
{
  "title": "标题",
  "contentText": "纯文本",
  "contentHtml": "<p>富文本</p>",
  "images": ["https://cdn.example.com/moments/img-1.jpg"]
}
```
- 约束:
- `title` 必填，最大 `80` 字
- `contentText` 和 `contentHtml` 至少一个非空

## 4. 点赞动态
- 方法: `POST /moments/{momentId}/likes`
- 请求体: 空
- 成功响应 `data`:
```json
{
  "liked": true,
  "likes": 13
}
```

## 5. 取消点赞
- 方法: `DELETE /moments/{momentId}/likes`
- 成功响应 `data`:
```json
{
  "liked": false,
  "likes": 12
}
```

## 5.1 管理-更新动态（仅作者）
- 方法: `PUT /moments/{momentId}`
- 请求体与发布一致:
```json
{
  "title": "更新后的标题",
  "contentText": "更新后的文本",
  "contentHtml": "<p>更新后的富文本</p>",
  "images": ["http://localhost:8080/files/image/6f4f5398989645c58924fdd0da0cf08b.jpg"]
}
```
- 权限: 只有该动态创建者可更新，非创建者返回 `403 MOMENT_FORBIDDEN`

## 5.2 管理-删除动态（仅作者）
- 方法: `DELETE /moments/{momentId}`
- 权限: 只有该动态创建者可删除，非创建者返回 `403 MOMENT_FORBIDDEN`

## 6. 获取评论分页
- 方法: `GET /moments/{momentId}/comments`
- Query 参数:
- `cursor`: 可选
- `size`: 可选，默认 `20`，范围 `1-50`
- `parentCommentId`: 可选，不传返回一级评论，传值返回该评论下回复
- `sort`: 可选，`latest | hot`，默认 `latest`

- 成功响应: `data` 为 `CursorPage<MomentCommentItem>`

## 7. 发表评论/回复
- 方法: `POST /moments/{momentId}/comments`
- 请求体:
```json
{
  "text": "评论内容",
  "parentCommentId": null,
  "replyToAccount": null
}
```
- 约束:
- `text` 长度 `1-500`
- 回复场景下 `replyToAccount` 必填
- 回复深度最多 2 层（一级评论 + 回复）

## 8. 获取点赞信息分页
- 方法: `GET /moments/{momentId}/likes`
- Query 参数:
- `cursor`: 可选
- `size`: 可选，默认 `20`，范围 `1-50`

- 成功响应: `data` 为 `CursorPage<MomentLikeUser>`

## 9. 图片上传
- 方法: `POST /files/upload`
- Content-Type: `multipart/form-data`
- 字段: `file`
- 约束:
- 仅支持图片类型
- 单图大小不超过 `10MB`

- 成功响应示例:
```json
{
  "code": 200,
  "status": "success",
  "message": "上传成功",
  "data": {
    "url": "http://localhost:8080/files/image/6f4f5398989645c58924fdd0da0cf08b.jpg",
    "objectName": "6f4f5398989645c58924fdd0da0cf08b.jpg",
    "width": 1080,
    "height": 720,
    "size": 245812
  }
}
```

## 10. 图片加载
- 方法: `GET /files/image/{objectName}`
- 鉴权: 无需登录（公开读取，便于前端 `<img src>` 直接加载）
- 返回: 图片二进制流，`Content-Type` 按上传文件类型返回

请求示例:
```http
GET /files/image/6f4f5398989645c58924fdd0da0cf08b.jpg HTTP/1.1
Host: localhost:8080
```

前端使用示例:
```html
<img src="http://localhost:8080/files/image/6f4f5398989645c58924fdd0da0cf08b.jpg" />
```

使用说明:
- 发布动态时先调用上传接口，取响应里的 `data.url`。
- 将 `data.url` 写入动态 `images[]` 字段即可。
- `data.objectName` 用于后续排障或二次处理（如缩略图、删除）。

## 错误码
- `400` + `MOMENT_INVALID_PARAM`: 参数非法
- `401` + `UNAUTHORIZED`: 未登录或 token 无效
- `403` + `MOMENT_FORBIDDEN`: 无权限管理该动态
- `404` + `MOMENT_NOT_FOUND`: 动态不存在
- `404` + `MOMENT_COMMENT_NOT_FOUND`: 评论不存在
- `500` + `INTERNAL_ERROR`: 服务端异常

## 说明
- 点赞/取消点赞为幂等操作。
- 时间字段为 ISO-8601 UTC。
- `friendStatusWithAuthor` 支持: `NONE | PENDING_OUTBOUND | PENDING_INBOUND | FRIEND`。
