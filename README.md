# boylu博客

一个前后端分离的博客系统，已移除原作者公开信息，并将默认站点身份调整为 `boylu`。

## 联系方式

- QQ：`3453619783`
- 邮箱：`3453619783@qq.com`
- 微信：`a3453619783`

## 项目结构

```text
blog/          后端 Spring Boot
blog-web/      前台 Web
blog-admin/    后台管理
uniapp-blog/   UniApp 客户端
mj-blog.sql    初始化数据库
```

## 本地运行

1. 导入根目录的 [mj-blog.sql](/E:/做题/shiyi-blog-master/mj-blog.sql)。
2. 修改 [application-dev.yml](/E:/做题/shiyi-blog-master/blog/mojian-server/src/main/resources/application-dev.yml) 中的数据库、Redis、邮箱等配置。
3. 启动后端服务，默认端口 `8800`。
4. 启动 [blog-web](/E:/做题/shiyi-blog-master/blog-web) 前台。
5. 启动 [blog-admin](/E:/做题/shiyi-blog-master/blog-admin) 后台。

## 默认本地地址

- 前台：`http://localhost:3000`
- 后台：`http://localhost:3001`
- 后端接口：`http://localhost:8800`

## Ubuntu 部署说明

当前默认配置已经去掉原作者域名。你上线到 Ubuntu 之前，只需要把以下文件里的本地地址替换成你自己的域名或反向代理地址：

- [blog-web/.env.production](/E:/做题/shiyi-blog-master/blog-web/.env.production)
- [blog-admin/.env.production](/E:/做题/shiyi-blog-master/blog-admin/.env.production)
- [application-dev.yml](/E:/做题/shiyi-blog-master/blog/mojian-server/src/main/resources/application-dev.yml)
