# 项目结构说明

本文档用于说明仓库目录职责，避免后续把临时文件、构建产物或业务代码放错位置。

## 一级目录

```text
blog/          后端 Spring Boot 多模块项目
blog-web/      博客前台 Vue2 + Vite 项目
blog-admin/    后台管理 Vue3 + Vite 项目
uniapp-blog/   UniApp 移动端项目
deploy/        部署配置、SQL 脚本和服务器相关文件
e2e/           Playwright 回归测试
docs/          项目文档、验收模板、结构说明
scripts/       本地辅助脚本
release/       发布包与人工归档，不建议长期提交大体积产物
local-backups/ 本地备份，不应进入版本库
```

## 后端 `blog/`

```text
blog/
  boylu-admin/    后台管理接口与后台业务服务
  boylu-api/      前台公开接口与前台业务服务
  boylu-auth/     登录、认证、第三方授权相关逻辑
  boylu-commom/   公共实体、DTO、VO、工具类、配置、Mapper 接口
  boylu-file/     文件上传与文件存储相关逻辑
  boylu-quartz/   定时任务相关逻辑
  boylu-server/   应用启动入口、配置文件、MyBatis XML、模板资源
```

放置规则：

- 控制器放对应模块的 `controller`。
- 业务接口放对应模块的 `service`。
- 业务实现放对应模块的 `service/impl`。
- 通用实体、DTO、VO、工具类优先放 `boylu-commom`。
- MyBatis XML 放 `boylu-server/src/main/resources/mapper`。
- 不要提交各模块的 `target`。

## 前台 `blog-web/`

```text
blog-web/
  src/api/          前台接口封装
  src/assets/       前台静态资源
  src/components/   可复用组件
  src/layout/       前台整体布局，例如 Header、Footer、MobileMenu
  src/router/       前台路由
  src/store/        Vuex 状态管理
  src/styles/       全局样式、变量、mixins
  src/utils/        前台工具函数
  src/views/        页面级视图
```

放置规则：

- 页面放 `src/views/<page>/index.vue`。
- 页面内局部组件放 `src/views/<page>/components/`。
- 跨页面复用组件放 `src/components/`。
- 接口请求统一放 `src/api/`。
- 通用工具函数放 `src/utils/`。
- 不要提交 `node_modules`、`dist` 和临时部署包。

## 后台 `blog-admin/`

```text
blog-admin/
  src/api/          后台接口封装
  src/components/   后台通用组件
  src/config/       后台配置
  src/layouts/      后台布局
  src/router/       后台路由
  src/store/        Pinia 状态管理
  src/styles/       后台全局样式
  src/types/        TypeScript 类型声明
  src/utils/        后台工具函数
  src/views/        后台页面
```

放置规则：

- 后台业务页面放 `src/views/<module>/<page>/`。
- 系统管理页面放 `src/views/system/`。
- 站点管理页面放 `src/views/site/`。
- 文章管理页面放 `src/views/article/`。
- 接口请求按业务模块放 `src/api/`。
- 不要提交 `node_modules`、`dist` 和临时部署包。

## 部署 `deploy/`

```text
deploy/
  nginx/      Nginx 配置
  sql/        初始化和升级 SQL
  systemd/    Linux systemd 服务配置
```

放置规则：

- 正式部署说明和可复用部署配置放这里。
- 临时部署包不要放这里。
- 数据库升级脚本按日期命名，例如 `20260419_xxx.sql`。

## 回归测试 `e2e/`

```text
e2e/
  tests/                 Playwright 测试用例
  playwright.config.ts   Playwright 配置
  .env.example           测试环境变量示例
```

放置规则：

- 测试用例放 `e2e/tests/`。
- 私有环境变量放 `e2e/.env.local`，不要提交。
- 不要提交 `e2e/node_modules`、`e2e/test-results`、`e2e/playwright-report`。

## 文档 `docs/`

建议放置：

- 项目结构说明
- 开发规范
- PR 验收模板
- 部署补充说明
- 常见问题记录

不建议放置：

- 构建产物
- 压缩包
- 临时备份代码

## 脚本 `scripts/`

建议放置：

- 本地启动脚本
- 回归测试脚本
- 配置辅助脚本
- 一次性但可复用的维护脚本

不建议放置：

- 临时部署包
- 已失效的一次性脚本
- 包含敏感信息的脚本

## 归档 `release/`

`release/` 只适合临时保存发布包或回滚包。长期建议：

- 只保留最近 1-2 次可用发布包。
- 大体积历史包移出 Git 仓库目录。
- 不要把 `release/` 纳入版本库。
