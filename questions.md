# 项目问题清单与处理进度（2026-05-14）

本轮优先处理博客功能问题，不处理服务器 SSH 连接问题。

## 已修复

### 图片加载链路

- `blog-web/src/components/common/SmartImage.vue` 已接入 `/img/local/<encoded>!w{width}.webp` 样式图链路，优先加载小 WebP 图，失败后回退旧原图，再回退默认图。
- `blog-web/src/views/article/index.vue` 文章详情正文图片已改为使用样式图懒加载，并保留 `data-origin` 作为预览和失败回退地址。
- `blog-web/vite.config.js` 和 `blog-admin/vite.config.ts` 已补上本地开发 `/img` 代理，避免开发环境样式图 404。
- `blog/boylu-file/src/main/java/com/boylu/controller/LocalImageStyleController.java` 已改为流式计算源图 hash，避免为 hash 一次性 `readAllBytes`。
- 样式图生成已增加源文件大小、像素总量、GIF 回退、透明 PNG 转 JPG 回退、WebP 保留 alpha、同一缓存文件生成锁。
- `blog/boylu-file/src/main/java/com/boylu/controller/FileController.java` 已增加 `img-cache` 统计和安全清理接口，只处理 `img-cache`，不删除旧原图。

### 性能与稳定性

- `blog/boylu-api/src/main/java/com/boylu/service/impl/HomeServiceImpl.java` 热榜接口已增加 Redis 缓存，减少第三方接口慢响应和限流影响。
- 旧文章封面回填前已增加文件大小和图片尺寸保护，源图过大或无法解析时跳过并记录日志。
- `blog/boylu-auth/src/main/java/com/boylu/service/impl/AuthServiceImpl.java` 修复了 Java 8 下 `URLEncoder.encode(..., Charset)` 编译不兼容问题。

### 安全收口

- `blog/boylu-admin/src/main/java/com/boylu/service/impl/SysArticleServiceImpl.java` 后台文章新增/更新已接入 `HtmlSanitizerUtil`，保存前清洗文章 HTML。
- `blog/boylu-commom/src/main/java/com/boylu/utils/HtmlSanitizerUtil.java` 已补充文章场景允许的图片懒加载属性和本地文件 URL 兼容。
- `blog/boylu-auth/src/main/java/com/boylu/config/satoken/SaTokenConfigure.java` 已移除全局 `/api/**` 放行，改为明确公开接口白名单；登录态接口重新走全局登录校验。

## 仍需确认或后续处理

### P0：Git 仓库状态仍不干净

- 当前 Git 中旧 `blog/mojian-*` 仍表现为大量删除，新 `blog/boylu-*` 仍是未跟踪目录。
- 如果要把当前后端正式同步到主分支，需要单独做一次清晰的 `mojian -> boylu` 迁移提交，避免远程仓库不可复现。
- 本轮同步 GitHub 时应只提交本轮博客问题相关文件，不应直接 `git add -A`。

### P1：图片 canonical 策略仍需最终定稿

- 当前运行策略偏向性能：本地文件优先返回 `/localFile/...`，由 Nginx 静态缓存。
- `docs/IMAGE_STORAGE.md` 仍倾向长期保存 `/boylu/file/content/{id}`。
- 后续需要统一文档和代码策略：是长期保存网关地址，还是长期保存本地静态地址。

### P2：图片处理仍可继续降内存

- 样式图接口已先加保护和降级，但 `ArticleCoverImageService`、分片完成后的上传流程仍以 `byte[]` 为主。
- 当前受上传大小和像素尺寸限制保护，后续如果图片量继续增大，可以再改为更完整的流式处理。

### P2：文档与临时目录

- `.tmp/` 里仍有不少临时验证、数据库探测和部署脚本，建议将可复用脚本移动到 `scripts/` 或 `deploy/`，一次性文件继续忽略。
- `docs/IMAGE_STORAGE.md` 和 Nginx 真实缓存策略仍需统一。
- `blog/pom.xml` 部分中文注释存在乱码，暂不影响构建，但建议后续整理为 UTF-8 中文或英文注释。

## 本轮验证

- `blog` 后端 Maven 编译已通过：
  `..\.tools\apache-maven-3.9.9\bin\mvn.cmd -pl boylu-server -am package -DskipTests`
- `blog-web` 前台构建已通过：`npm run build`。
- `blog-admin` 后台构建已通过：`npm run build`。
- 前台构建仅保留 `mavon-editor` 依赖自身的 eval 警告；后台构建仅保留 Sass legacy API 和大 chunk 警告。
