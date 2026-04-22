# Playwright 回归测试（博客专用）

这套用例覆盖 4 个核心场景：

1. `blog-web` 登录页
2. `blog-web` 文章详情页（从首页进入）
3. `blog-web` 相册页
4. `blog-admin` 管理后台首页与相册管理页（需要登录态 token）

## 安装依赖

```bash
cd e2e
npm install
npm run install:browsers
```

## 配置环境变量

```powershell
Copy-Item .env.example .env.local
```

默认端口已按项目写好：

- `WEB_BASE_URL=http://127.0.0.1:3000`
- `ADMIN_BASE_URL=http://127.0.0.1:3001`

`ADMIN_TOKEN` 用于后台鉴权测试。获取方式：

1. 先手动登录后台 `http://127.0.0.1:3001/login`
2. 打开浏览器开发者工具 -> Application/Storage -> Cookies
3. 找到 cookie `boylu-blog-admin-token`，把值填到 `.env.local` 的 `ADMIN_TOKEN`

未配置 `ADMIN_TOKEN` 时，后台鉴权测试会自动跳过，不影响前台回归。

## 启动被测服务

```bash
cd blog-web
npm run dev
```

```bash
cd blog-admin
npm run dev
```

## 运行测试

```bash
cd e2e
npm test
```

常用命令：

```bash
npm run test:list
npm run test:headed
npm run test:ui
```
