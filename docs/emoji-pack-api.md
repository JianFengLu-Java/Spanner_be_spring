# 表情包图库与收藏 API

## 认证
- 除特殊说明外，接口都需要 `Authorization: Bearer <access_token>`。

## 1. 上传表情到图库
- `POST /emojis/gallery/upload`
- `Content-Type: multipart/form-data`
- 参数：
  - `file`（必填，图片文件，支持 `jpg/png/webp/gif`，最大 10MB）
  - `displayName`（可选，表情名称，最长 80）

### 响应示例
```json
{
  "code": 200,
  "status": "success",
  "message": "上传表情到图库成功",
  "data": {
    "id": "e9b4a9893ec64dc5af2ad2c44f3d57e5",
    "displayName": "开心",
    "imageUrl": "http://spanner.top:9000/pic/xxxyyyzzz.png",
    "objectName": "xxxyyyzzz.png",
    "contentType": "image/png",
    "fileSize": 120345,
    "width": 300,
    "height": 300,
    "favoriteCount": 0,
    "favorited": false,
    "createdAt": "2026-02-21T08:00:00Z"
  }
}
```

> 前端上传后使用 `data.imageUrl` 作为图片链接即可。

## 2. 查询我的图库
- `GET /emojis/gallery/my?page=1&size=20&keyword=开心`
- 参数：
  - `page`（默认 1）
  - `size`（默认 20，最大 100）
  - `keyword`（可选，按名称模糊搜索）

## 3. 删除图库表情
- `DELETE /emojis/gallery/{emojiId}`

## 4. 收藏表情
- `POST /emojis/favorites/{emojiId}`

## 5. 取消收藏
- `DELETE /emojis/favorites/{emojiId}`

## 6. 查询我的收藏
- `GET /emojis/favorites?page=1&size=20`

## 通用错误码
- `400 EMOJI_INVALID_PARAM` 参数非法
- `401 UNAUTHORIZED` 未登录/登录失效
- `404 EMOJI_NOT_FOUND` 表情不存在
- `500 INTERNAL_ERROR` 服务器内部错误
