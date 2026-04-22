# boylu-blog-master

这是一个个人博客全栈项目，包含前台博客、后台管理、后端服务、移动端、部署配置和回归测试。

## 快速认识目录

```text
blog/          Spring Boot 后端多模块项目
blog-web/      博客前台 Vue2 + Vite 项目
blog-admin/    后台管理 Vue3 + Vite 项目
uniapp-blog/   UniApp 移动端项目
deploy/        Nginx、systemd、SQL 等部署文件
e2e/           Playwright 回归测试
docs/          项目文档、验收模板、结构说明
scripts/       本地辅助脚本
mj-blog.sql    数据库初始化脚本
```

更详细的目录职责见 [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)。

## 本地启动

1. 导入 `mj-blog.sql`。
2. 修改后端开发配置：`blog/mojian-server/src/main/resources/application-dev.yml`。
3. 启动后端服务，默认端口 `8800`。
4. 进入 `blog-web`，安装依赖并启动前台。
5. 进入 `blog-admin`，安装依赖并启动后台。

```powershell
cd blog-web
npm install
npm run dev
```

```powershell
cd blog-admin
npm install
npm run dev
```

## 默认地址

- 前台：`http://localhost:3000`
- 后台：`http://localhost:3001`
- API：`http://localhost:8800`

## 维护约定

- 源码只放在各项目的 `src` 或后端模块标准目录内。
- 构建产物、依赖目录、临时部署包不要提交到仓库。
- 临时回滚包统一放到 `release/` 或移出仓库目录。
- 新增脚本放 `scripts/`，新增说明放 `docs/`。
- 清理和提交前先查看 `git status --short`，确认没有误提交 `node_modules`、`dist`、`target`、压缩包。

## 联系方式

- QQ：`3453619783`
- Email：`3453619783@qq.com`
- WeChat：`a3453619783`
