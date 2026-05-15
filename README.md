# boylu-blog-master

一个个人博客全栈项目，包含前台博客、后台管理、Spring Boot 后端、部署配置和回归测试脚本。

## 项目结构

```text
blog/          Spring Boot 后端 Maven 多模块项目
blog-web/      前台博客，Vue 2 + Vite
blog-admin/    后台管理，Vue 3 + Vite + Element Plus
uniapp-blog/   UniApp 移动端项目
deploy/        Nginx、systemd、SQL、部署辅助脚本
docs/          项目文档、结构说明、维护说明
scripts/       本地辅助脚本
e2e/           Playwright 回归测试
mj-blog.sql    数据库初始化脚本
questions.md   当前排查过的问题和处理记录
```

更详细的目录职责见 [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)，源码摆放规则见 [docs/SOURCE_STRUCTURE_GUIDE.md](docs/SOURCE_STRUCTURE_GUIDE.md)。

## 环境要求

- JDK 8
- Maven 3.8+
- Node.js 18+
- MySQL 5.7 / 8.x
- Redis 6+
- npm

## 本地启动

### 1. 克隆项目

```bash
git clone https://github.com/ste3fs/boylu-blog-master.git
cd boylu-blog-master
```

### 2. 初始化数据库

先创建数据库，再导入初始化 SQL。

```sql
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bash
mysql -u root -p blog < mj-blog.sql
```

如果你的 MySQL 用户名、密码不是 `root/root`，不要改代码，优先用环境变量覆盖：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
```

Linux / macOS：

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

### 3. 启动 Redis

本地默认读取：

```text
host: 127.0.0.1
port: 6379
database: 8
```

如果 Redis 没启动，后端会启动失败或部分缓存功能异常。

### 4. 启动后端

```bash
cd blog
mvn -pl boylu-server -am spring-boot:run
```

后端默认地址：

```text
http://localhost:8800
```

如果只想打包：

```bash
cd blog
mvn -pl boylu-server -am package -DskipTests
```

打包产物：

```text
blog/boylu-server/target/boylu-blog.jar
```

### 5. 启动前台博客

```bash
cd blog-web
npm install
npm run dev
```

前台默认地址：

```text
http://localhost:3000
```

### 6. 启动后台管理

```bash
cd blog-admin
npm install
npm run dev
```

后台本地默认地址：

```text
http://localhost:3001/login
```

生产环境后台默认入口是 `/boylu1107/login`，不是 `/admin`。

## 本地接口代理

前台开发环境：

```text
blog-web/.env.development
VITE_APP_BASE_API=/dev-api
VITE_APP_API_URL=http://localhost:8800
```

后台开发环境：

```text
blog-admin/.env.development
VITE_APP_BASE_API=/api
VITE_APP_API_URL=http://localhost:8800/
```

如果你修改了后端端口，要同步修改这两个文件里的 `VITE_APP_API_URL`。

## 常用配置

后端开发配置文件：

```text
blog/boylu-server/src/main/resources/application-dev.yml
```

生产配置文件：

```text
blog/boylu-server/src/main/resources/application-prod.yml
```

生产环境建议使用环境变量，不要把真实密码、密钥、服务器 IP、邮箱授权码提交到 GitHub。常见变量：

```text
PUBLIC_BASE_URL=https://your-domain.example
FRONTEND_BASE_URL=https://your-domain.example
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=blog
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DATABASE=8
MAIL_EMAIL=<MAIL_EMAIL>
MAIL_PASSWORD=<MAIL_PASSWORD>
AI_API_KEY=<AI_API_KEY>
```

完整示例见：

```text
deploy/systemd/boylu-blog.env.example
```

## 生产构建

前台：

```bash
cd blog-web
npm run build
```

后台：

```bash
cd blog-admin
npm run build
```

后端：

```bash
cd blog
mvn -pl boylu-server -am package -DskipTests
```

部署到 Ubuntu 的完整步骤见 [deploy/DEPLOY_UBUNTU.md](deploy/DEPLOY_UBUNTU.md)。

## 图片与上传文件

项目兼容这些历史图片地址：

```text
/localFile/...
/file/content/{id}
/file/view/{id}
/boylu/file/content/{id}
/mojian/file/content/{id}
```

新业务字段优先保存：

```text
/boylu/file/content/{fileId}
```

生产环境上传目录建议放在：

```text
/opt/boylu-blog/storage/
```

迁移服务器时，需要同时迁移数据库和上传文件目录。更多说明见 [docs/IMAGE_STORAGE.md](docs/IMAGE_STORAGE.md)。

## 隐私与安全注意事项

公开仓库里不要提交：

- 服务器真实 IP
- SSH 用户名和密码
- 数据库密码
- Redis 密码
- 邮箱授权码
- 第三方登录 App Secret
- AI API Key
- 真实支付二维码
- 私钥文件，例如 `id_rsa`、`*.pem`、`*.key`

本仓库中的部署文件只保留占位符。你自己的生产配置应该放到服务器环境变量或 `/etc/boylu-blog/boylu-blog.env`，不要提交回仓库。

## 常见问题

### 后端连接数据库失败

确认 MySQL 已启动、数据库 `blog` 已创建、`mj-blog.sql` 已导入，并检查 `DB_USERNAME`、`DB_PASSWORD`。

### 前端页面请求 404

确认后端运行在 `http://localhost:8800`，并检查 `blog-web/.env.development` 和 `blog-admin/.env.development` 的 `VITE_APP_API_URL`。

### 图片不显示

确认上传文件目录存在，数据库里的文件记录存在，并检查 `/localFile/...` 或 `/boylu/file/content/{id}` 是否能访问。

### 后台访问地址不对

本地后台是：

```text
http://localhost:3001/login
```

生产后台默认是：

```text
https://your-domain.example/boylu1107/login
```

`/admin` 在生产 Nginx 中可以被故意隐藏，不要用它做健康检查。

## 维护建议

提交前建议至少执行：

```bash
cd blog-web && npm run build
cd ../blog-admin && npm run build
cd ../blog && mvn -pl boylu-server -am package -DskipTests
```

如果改动涉及登录、资源上传、文章图片、相册或后台菜单，请同时更新 `questions.md` 或相关文档，方便后续排查。
