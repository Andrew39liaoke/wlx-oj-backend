```
- API Gateway: 作为统一的认证中心，所有的请求需经过网关，对 Token 进行验证，非法则报401错误
- Auth Service：作为专门的授权服务器，负责处理用户登录、注册等认证相关操作以及生成和管理 OAuth2 Token 令牌
- 后台业务服务：负责处理具体的业务逻辑，不直接集成 OAuth2 组件，而是信任来自 API Gateway 的请求
- 前端应用：在 API 请求头中加入 Token 令牌，Token 过期重新登录或者 调用 Refresh Token
```

```
- API Gateway: 对于认证通过的 API 请求，对登录用户所属角色下的 API 权限资源列表进行校验，判断是否有权限访问
- Auth Service：基于 RBAC 等权限模型设计，保存用户的权限信息，并提供接口给 API Gateway 和前端应用调用
- 前端应用：获取 Auth Service 的用户功能权限数据，渲染用户可以看到的菜单、页面、按钮等
```

​    

nacos链接：http://localhost:8848/nacos/index.html

Rabbitmq链接：[RabbitMQ: Overview](http://127.0.0.1:15672/#/)



#### AI写前端提示词

- 使用UI UX Pro Max工具 https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

  <img src="D:\note\assets\image-20260117132639551.png" alt="image-20260117132639551" style="zoom:67%;" /> 

#### 前端生成后端的代码

```
执行generated-js的文件
```



#### 白名单url

##### 帖子服务

- /api/post/comment/get/{id}
- /api/post/getInfo/{id}
- /api/post/page

##### 题目服务

- /api/question/get/vo

- /api/question/list/page/vo



#### 管理员url

- /api/question/update



#### 代实现后端接口

- 题目的点赞与取消点赞
- 题目的收藏与取消收藏

- 管理员对于用户的增删改查



