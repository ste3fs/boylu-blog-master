# 项目问题清单与处理进度（2026-05-14）

本轮优先检查线上可见功能问题，并先处理首页随机视频播放器位置和功能异常。

## 2026-05-14 当前检查问题

### P1：随机视频播放器位置和功能异常（已修复，待部署复核）

- 线上表现：随机视频内容直接渲染到页面底部，遮挡页脚、右侧悬浮按钮和页面内容。
- GitHub 旧版对比：旧版 `RandomVideo` 的意图是左侧箭头触发播放器抽屉，播放器不应该常驻底部横向铺满。
- 根因判断：前台 Element UI 当前只按需注册了部分组件，`RandomVideo` 使用的 `el-drawer`、`el-tooltip` 没有全局注册，组件会被浏览器当成普通未知标签输出；同时视频源仍是 `http://`，在 HTTPS 站点存在混合内容拦截风险。
- 修复方向：改为组件内部的左侧固定小播放器，默认只显示左侧箭头，点击后展开；关闭时暂停播放，切换视频时重新拉取；视频源改为 HTTPS。
- 处理结果：`blog-web/src/components/RandomVideo/index.vue` 已改为自包含左侧小面板，不再依赖未注册的 `el-drawer/el-tooltip`；`npm run build` 已通过。

### P2：已复核的历史记录

- 普通上传流程已改为先写入临时文件，再读取文件头和图片元数据校验，不再通过 `MultipartFile#getBytes()` 读整图。
- 本轮前台构建未再出现 `mavon-editor` eval 警告，该项不再作为待处理问题保留。

## 已修复

### Git 仓库迁移

- 已在干净 worktree 中单独完成 `mojian -> boylu` 后端迁移提交。
- 后端模块统一为 `blog/boylu-*`，Java 包名统一为 `com.boylu`。
- 旧 `blog/mojian-*` 不再作为当前修改目标。

### 图片 canonical 策略

- 最终策略已定稿：业务字段长期保存 `/boylu/file/content/{fileId}`。
- 上传接口和文章封面响应式变体返回值统一为网关地址，不再因为本地存储而优先返回 `/localFile/...`。
- 运行时仍保留性能策略：网关地址解析真实文件后跳转或读取 `/localFile/...`，由 Nginx 静态缓存返回。
- `docs/IMAGE_STORAGE.md` 已同步该策略，并把 `/localFile/` 缓存说明改为 365 天 immutable。

### 图片加载链路

- `SmartImage.vue` 已接入 `/img/local/<encoded>!w{width}.webp` 样式图链路，优先加载小 WebP 图。
- 文章详情正文图片已改为懒加载样式图，并保留 `data-origin` 作为预览和失败回退地址。
- `blog-web` 和 `blog-admin` 本地开发代理已补齐 `/img`。
- `LocalImageStyleController` 已改为流式计算源图 hash，避免为 hash 一次性 `readAllBytes`。
- 样式图生成已增加源文件大小、像素总量、GIF 回退、透明 PNG 转 JPG 回退、WebP 保留 alpha 和同一缓存文件生成锁。
- `FileController` 已增加 `img-cache` 统计和安全清理接口，只处理 `img-cache`，不删除旧原图。

### 图片处理降内存

- 分片上传完成后不再 `Files.readAllBytes(mergedPath)`，改为基于临时合并文件上传。
- 分片上传完成后的图片类型识别和尺寸校验改为读取文件头和图片元数据。
- 普通上传流程已改为 `copyUploadToTempFile`，通过 `MultipartFile#getInputStream()` 写入临时文件后再进入校验和上传。
- 旧文章封面回填不再把源图读成 `byte[]` 再交给封面服务，改为传入 `Path`。
- `ArticleCoverImageService` 新增 `Path` 处理入口，源图 hash 改为流式计算，原图上传走本地文件输入。
- 封面变体生成仍需要解码为 `BufferedImage`，这是当前生成多尺寸 WebP/JPG/AVIF 的必要内存占用。

### 性能与稳定性

- 热榜接口已增加 Redis 缓存，减少第三方接口慢响应和限流影响。
- 旧文章封面回填前已增加文件大小和图片尺寸保护，源图过大或无法解析时跳过并记录日志。
- 首页 Redis 缓存反序列化、分类快速切换、阅读时间、统计展示等问题已按前一轮记录修复。

### 安全收口

- 后台文章新增/更新已接入 `HtmlSanitizerUtil`，保存前清洗文章 HTML。
- `HtmlSanitizerUtil` 已补充文章场景需要的图片懒加载属性和本地文件 URL 兼容。
- `SaTokenConfigure` 已移除全局 `/api/**` 放行，改为明确公开接口白名单。
- 部署脚本不再硬编码服务器密码，改为读取 `BOYLU_SSH_PASSWORD` 或使用 SSH key / ssh-agent。

### 文档与临时目录

- `.gitignore` 已加入 `.tmp/`，临时验证、数据库探测和一次性部署脚本默认不进入仓库。
- 可复用脚本后续应移动到 `scripts/` 或 `deploy/`。
- `blog/pom.xml` 乱码中文注释已改为清晰英文注释。

## 仍需关注

### 图片处理内存

- 文章封面多尺寸生成仍需要将源图解码为 `BufferedImage`，如果后续图片量或并发继续增大，可以继续做更完整的流式/队列化处理。

### 前端构建体积

- 前台构建本轮已复核通过，未再出现 `mavon-editor` 自身 eval 警告。
- 后台构建存在 Sass legacy API 和大 chunk 警告。
- 这些目前不影响构建结果，后续可单独优化依赖拆分。

## 本轮验证结果

- `blog` Maven 编译已通过：`mvn -pl boylu-server -am package -DskipTests`。
- `blog-web` 构建已通过：`npm run build`。
- 2026-05-14 随机视频播放器修复后，`blog-web` 构建再次通过：`npm run build`。
- `blog-admin` 构建已通过：`npm run build`。
- `blog-web` 安装依赖时保留 npm audit 提示：20 个漏洞提示。
- `blog-admin` 安装依赖时保留 npm audit 提示：11 个漏洞提示。
- 前台构建本轮未再出现 `mavon-editor` eval 警告；后台构建保留 Sass legacy API 和大 chunk 警告。
- 部署后检查：
  - `sudo systemctl status boylu-blog --no-pager`
  - `sudo nginx -t`
  - 首页 `全部` 分类、相册页、文章详情页、留言板。
  - `/boylu/api/article/home-list?pageNum=1&pageSize=10` 连续请求不返回 500。
  - `/boylu/file/content/{id}` 能访问并最终命中 `/localFile/...`。
  - `/img/local/<encoded>!w320.webp` 能返回样式图或正确回退。
