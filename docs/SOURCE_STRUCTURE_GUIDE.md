# 源码结构维护指南

这份文档约束源码层面的文件摆放和命名，避免后续功能越写越散。

## 前台 `blog-web`

`blog-web` 是 Vue 2 + Vite 的博客前台，目录职责如下：

- `src/views`：页面级组件，和路由一一对应，目录统一使用小写命名，例如 `home`、`article`、`login`。
- `src/components`：跨页面复用组件，例如评论、搜索、侧边栏、AI 浮窗等。
- `src/layout`：前台整体壳层组件，例如 Header、Footer、MobileMenu。
- `src/api`：接口请求文件，按业务域拆分，例如 `article.js`、`album.js`、`auth.js`。
- `src/store`：Vuex 全局状态，只放跨页面共享状态。
- `src/utils`：纯工具函数，不直接承载页面业务逻辑。
- `src/styles`：全局样式、变量、mixin，页面私有样式优先留在对应 `.vue` 文件内。

新增页面时优先遵循：

- 页面放入 `src/views/<page-name>/index.vue`。
- 路由统一在 `src/router/index.js` 注册。
- 需要登录的路由统一使用 `meta.requireAuth = true`。
- 不再使用 `requiresAuth`、`needLogin` 等其他鉴权字段。
- 页面内部独有的小组件放到当前页面的 `components` 子目录。
- 多页面复用超过两次的组件再上提到 `src/components`。

## 后台 `blog-admin`

`blog-admin` 是 Vue 3 + Vite + TypeScript 的管理后台，目录职责如下：

- `src/views`：后台页面，按菜单模块分组，例如 `article`、`site`、`system`。
- `src/api`：后台接口，按后端模块分组。
- `src/store/modules`：Pinia 模块，自动导入配置以此目录为准。
- `src/layouts`：后台框架布局。
- `src/components`：后台通用组件。
- `src/plugins`：插件注册、权限、图标等启动期配置。
- `src/icons/svg`：自定义 SVG 图标源文件。

后台不再使用 Webpack 风格的 `require.context` 图标入口。当前项目使用 `vite.config.ts` 中的 `svgBuilder('./src/icons/svg/')` 处理 SVG。

## 后端 `blog`

`blog` 是 Spring Boot 多模块后端，模块边界如下：

- `mojian-server`：启动模块和资源配置。
- `mojian-api`：前台公开接口。
- `mojian-admin`：管理后台接口。
- `mojian-auth`：登录、鉴权、Sa-Token 相关逻辑。
- `mojian-commom`：公共实体、DTO、VO、工具类、Mapper。
- `mojian-file`：文件上传和存储。
- `mojian-quartz`：定时任务。

后端维护规则：

- 控制器按业务域放到对应模块的 `controller/<domain>`。
- 服务实现放到 `service/impl`，接口放到 `service`。
- 公共 DTO、VO、Entity、Mapper 继续放在 `mojian-commom`。
- 不在生产代码里使用 `System.out.println` 和普通 `printStackTrace()`。
- 需要输出运行信息时使用日志框架，正常流程用 `info` 或 `debug`，异常用 `error`。

## 清理原则

- 可以直接删除：无引用的旧说明文件、无引用且与当前构建工具不兼容的入口文件。
- 不要随意移动：路由页面、接口文件、后端包路径、Mapper XML。
- 要先验证再移动：组件上提、API 合并、工具函数拆分。
- 构建产物和依赖目录不进 Git：`dist`、`target`、`node_modules`。
