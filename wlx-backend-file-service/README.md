# 文件服务使用说明

## 功能概述

文件服务提供了完整的文件上传、下载和管理功能，支持阿里云OSS存储。

## 主要功能

### 1. 文件上传
- 支持多种文件类型（图片、文档等）
- 文件大小限制验证
- 文件类型验证
- 自动生成唯一文件名
- 支持业务类型分类存储

### 2. 文件删除
- 根据文件ID删除文件
- 同时删除OSS存储和数据库记录
- 权限验证（只能删除自己的文件）

### 3. 文件信息查询
- 根据文件ID获取文件信息

## API接口

### 外部接口（需要登录）

#### 上传文件
```
POST /api/file/upload
Content-Type: multipart/form-data

参数：
- file: MultipartFile 文件
- biz: String 业务类型（默认"default"）

响应：
{
    "code": 0,
    "data": {
        "url": "文件访问URL",
        "fileName": "原始文件名",
        "fileSize": 12345
    }
}
```

#### 删除文件
```
POST /api/file/delete
Content-Type: application/x-www-form-urlencoded

参数：
- fileId: Long 文件ID

响应：
{
    "code": 0,
    "data": true
}
```

#### 获取文件信息
```
GET /api/file/info?fileId=123

响应：
{
    "code": 0,
    "data": {
        "id": 123,
        "fileName": "example.jpg",
        "fileSize": 12345,
        "fileType": "image/jpeg",
        "fileUrl": "https://...",
        "userId": 1,
        "createTime": "2024-01-01T00:00:00"
    }
}
```

### 内部接口（服务间调用）

#### 上传文件
```
POST /api/file/inner/upload
Content-Type: multipart/form-data

参数：
- file: MultipartFile 文件
- biz: String 业务类型
- userId: Long 用户ID
```

#### 删除文件
```
POST /api/file/inner/delete

参数：
- fileId: Long 文件ID
- userId: Long 用户ID
```

#### 获取文件信息
```
GET /api/file/inner/info?fileId=123
```

## 配置说明

### application.yml 配置

```yaml
# 阿里云OSS配置
aliyun:
  oss:
    endpoint: your-oss-endpoint
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
    region: your-region

# 文件上传配置
file:
  upload:
    # 允许的文件类型
    allowed-types:
      - image/jpeg
      - image/png
      - image/gif
      - application/pdf
    # 最大文件大小（字节）
    max-size: 104857600  # 100MB
    # 文件存储路径前缀
    path-prefix: files/
    # 文件访问URL前缀
    url-prefix: https://your-bucket-name.oss-cn-region.aliyuncs.com/
```

## 使用示例

### 上传头像

```java
// 直接调用文件服务的头像上传接口
POST /api/file/upload/avatar
Content-Type: multipart/form-data

// 请求参数
file: MultipartFile (头像文件)

// 响应
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

### 上传普通文件

```java
// 直接调用文件服务的文件上传接口
POST /api/file/upload
Content-Type: multipart/form-data

// 请求参数
file: MultipartFile (文件)
biz: String (业务类型，默认"default")

// 响应
{
  "code": 0,
  "data": {
    "url": "https://oss.example.com/files/default/uuid.jpg",
    "fileId": 123
  },
  "message": "ok"
}
```

### 删除文件

```java
POST /api/file/delete

// 请求参数
fileId: Long (文件ID)

// 响应
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

### 获取文件信息

```java
GET /api/file/info

// 请求参数
fileId: Long (文件ID)

// 响应
{
  "code": 0,
  "data": {
    "id": 123,
    "fileName": "avatar.jpg",
    "fileSize": 1024,
    "fileType": "image/jpeg",
    "url": "https://oss.example.com/files/avatar/uuid.jpg"
  },
  "message": "ok"
}
```

## 文件存储结构

```
OSS Bucket/
├── files/
│   ├── user_avatar/
│   │   ├── 2024/01/
│   │   │   ├── uuid1.jpg
│   │   │   └── uuid2.png
│   ├── post_image/
│   │   ├── 2024/01/
│   │   │   ├── uuid3.jpg
│   │   │   └── uuid4.png
```

## 注意事项

1. 上传前请确保阿里云OSS配置正确
2. 文件大小和类型会进行验证
3. 删除文件会同时删除OSS和数据库记录
4. 内部接口主要用于服务间调用，无需登录验证
5. 建议根据业务类型设置不同的biz参数，便于文件管理
