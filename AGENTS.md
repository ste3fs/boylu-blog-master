# AGENTS.md

## 项目信息

- 项目名称：boylu 个人博客
- 本地路径：`<PROJECT_ROOT>`
- 线上服务器：`<SERVER_IP>`
- SSH 用户：`<SSH_USER>`
- 前台站点目录：`/var/www/boylu-blog/`
- 后台管理目录：`/var/www/boylu-blog/admin/`
- 后台线上入口：`/boylu1107`；`/admin` 被 Nginx 故意返回 404，不要作为后台健康检查地址。
- 后端服务目录：`/opt/boylu-blog/server/`
- 上传文件目录：`/opt/boylu-blog/storage/`
- 后端服务名：`boylu-blog`
- Nginx 站点配置：`/etc/nginx/sites-enabled/boylu-blog.conf`
- Naxsi 日志：`/var/log/nginx/naxsi_public.log`
- Naxsi 日报脚本：`/usr/local/bin/naxsi_daily_report.py`

不要把服务器密码、数据库密码、Token 写入本文档、脚本或提交记录。部署脚本应使用 `BOYLU_SSH_PASSWORD`、SSH key 或 ssh-agent。

## 当前有效代码目录

- 前台前端：`blog-web`
- 后台前端：`blog-admin`
- 后端 Maven 工程：`blog/boylu-*`
- 当前有效后端入口：`blog/boylu-server`
- 旧的 `blog/mojian-*` 目录不是当前修改目标，不要恢复或混用。

## 部署方式

- 前台构建：在 `blog-web` 执行 `npm run build`
- 后台构建：在 `blog-admin` 执行 `npm run build`
- 后端构建：在 `blog` 下执行 `<PROJECT_ROOT>\.tools\apache-maven-3.9.9\bin\mvn.cmd -pl boylu-server -am package -DskipTests`
- 前台构建产物同步到：`/var/www/boylu-blog/`
- 后台构建产物同步到：`/var/www/boylu-blog/admin/`
- 后端 jar 同步到：`/opt/boylu-blog/server/boylu-blog.jar`
- 更新后重启服务：`sudo systemctl restart boylu-blog`
- 重载 Nginx 前必须先执行：`sudo nginx -t`

## 图片和旧数据规则

- 不删除、不移动、不覆盖 `/opt/boylu-blog/storage/` 中的旧图片。
- 业务字段、文章内容、相册、头像和封面元数据长期保存 canonical 地址：`/boylu/file/content/{fileId}`。
- `/localFile/...` 是真实静态文件地址和历史兼容地址，不作为新业务字段的首选保存格式。
- 必须继续兼容旧图片地址：
  - `/localFile/...`
  - `/file/content/{id}`
  - `/file/view/{id}`
  - `/boylu/file/content/{id}`
  - `/boylu/file/view/{id}`
  - `/mojian/file/content/{id}`
  - `/mojian/file/view/{id}`
- 运行时本地文件仍优先落到 `/localFile/...`，由 Nginx 静态缓存返回。
- 本地图片样式读取：`/img/local/<base64url旧地址>!w320.webp`，首次请求由后端生成 `/localFile/img-cache/...` 缓存图，后续由 Nginx 静态缓存返回。
- 图片缓存只新增到 `img-cache`，按原图内容 hash、宽度、格式命名；不删除、不覆盖任何旧原图。
- 旧文章封面回填时，如果源文件不存在，只跳过并记录日志，不允许把原封面地址覆盖成默认图。
- 默认头像使用：`/boylu-avatar.jpg`

## 本轮改动记录

### 留言、评论、头像和旧图片

- 留言板前端不再提交 `createTime`，只提交内容、昵称、头像、浏览器等必要字段。
- 留言后端兜底游客昵称和头像，由后端/数据库生成创建时间。
- 文章评论仍要求登录；未登录或 token 过期时提示“请先登录后评论”。
- 评论通知发送失败时只记录日志，不影响评论写入。
- 首页文章作者头像兜底顺序：`post.avatar -> webSiteInfo.authorAvatar -> webSiteInfo.touristAvatar -> /boylu-avatar.jpg`。
- 相册列表、相册详情和文章图片接入 `SmartImage`，保留旧 URL 兼容和错误回退。
- 文件旧接口找不到文件时返回 404 或清缓存重查，不抛空指针。

### 热榜修复

- 修复 `/api/getHotSearch/{type}` 返回空数组导致热榜无数据的问题。
- 后端保留原 `CoderUtil` 逻辑，密钥缺失、请求失败或返回空数据时切换到免密兜底源。
- 覆盖 `weibo`、`zhihu`、`toutiao`、`baidu`、`csdn` 五个热榜类型。
- 热榜接口已增加 Redis 缓存，减少第三方接口慢响应和限流风险。

### 首页体验修复

- 修复首页 `全部` 分类偶发卡住：Redis 首页缓存中的 `records` 会被显式转换为 `HomeArticleVo`，失败时删除缓存并回源数据库。
- 修复快速切换分类时旧请求覆盖新请求的问题，首页只接收最后一次文章列表请求结果。
- 修复图片灰块：`SmartImage` 响应式图片失败后先回退旧封面/原图地址，再回退默认图。
- 首页阅读时间恢复优先使用后端 `readingTime` 字段；字段缺失时才用前端摘要兜底估算。
- 删除右下角“AI 提问”悬浮框；顶部导航里的“AI 助手”入口保留。
- 今日状态显示“今日访客 / 总访客”。

### 淘宝式图片加载和图片缓存

- 后端新增 `/img/local/{encoded}!w{width}.{format}` 图片样式接口，支持 `160/240/320/480/640/960/1280` 宽度和 `webp/jpg` 输出。
- 后端登录拦截白名单已放行 `/img/**`，图片样式读取不要求登录。
- 图片样式接口只读取兼容地址，不修改旧 URL 和数据库原字段。
- 前端 `SmartImage` 默认优先请求 `320/480/640` WebP 小图，失败时回退旧原图，再回退默认图。
- 首页文章列表第一页前两张图设置高优先级，其余图片懒加载。
- 文章详情富文本图片渲染时动态改为小 WebP 地址，同时保留 `data-origin` 原图用于预览和失败回退。
- Nginx 增加 `/img/` 代理到后端生成入口；生成后的 `/localFile/img-cache/...` 继续由 Nginx 静态缓存一年。

### 图片 canonical 定稿

- 最终策略：长期保存 `/boylu/file/content/{fileId}`，运行时再解析到 `/localFile/...` 或外部对象存储地址。
- `FileController` 上传返回值统一为网关地址；如果能解析真实目标，会把目标写入 Redis 缓存，但不再把 `/localFile/...` 作为上传响应返回。
- `ArticleCoverImageService` 的原图和响应式变体也统一返回 `/boylu/file/content/{fileId}`。
- `docs/IMAGE_STORAGE.md` 已同步 canonical 策略，并把 `/localFile/` 缓存说明改为 365 天 immutable。

### 图片处理降内存

- `LocalImageStyleController` 已避免为 hash 一次性读取原图，增加源图大小/像素保护、GIF 回退、透明 PNG 转 JPG 回退、WebP 保留 alpha 和同一缓存文件生成锁。
- 分片上传完成后不再 `Files.readAllBytes(mergedPath)`，改为基于临时合并文件上传。
- 分片上传完成后的图片类型识别和尺寸校验改为读取文件头和图片元数据。
- 旧文章封面回填不再把源图读成 `byte[]`，改为传入 `Path`。
- `ArticleCoverImageService` 新增 `Path` 处理入口，源图 hash 改为流式计算，原图上传走本地文件输入。
- 封面多尺寸变体生成仍需要解码源图为 `BufferedImage`，后续如果图片量和并发继续增大，可以再做队列化或更完整的流式处理。

### 个人中心功能修复

- 个人资料表单恢复真实校验，保存前执行 Element 表单校验。
- 个人资料保存只提交允许字段；后端忽略前端传入的用户 ID，统一按当前登录用户更新。
- 重置按钮恢复为“回到本次加载的用户资料”。
- 头像上传成功后同步更新个人中心表单、侧边资料卡和 Vuex 用户信息。
- 账号绑定页移除前端假绑定数据；当前后端没有安全绑定接口时只提示暂不支持。
- 个人中心修改密码接口取消后台权限码限制，只要求登录并校验旧密码。
- 个人中心意见反馈允许普通登录用户提交；普通用户只能看自己的反馈。
- 个人中心“我的评论/我的回复”内容渲染增加 HTML 清洗。
- 删除我的评论时后端会校验评论属于当前登录用户。

### 仓库与文档整理

- 已在干净 worktree 单独提交一次清晰的 `mojian -> boylu` 后端迁移。
- `.gitignore` 已加入 `.tmp/`，临时验证、数据库探测和一次性部署脚本默认不进入仓库。
- 可复用脚本后续放到 `scripts/` 或 `deploy/`。
- `blog/pom.xml` 乱码中文注释已改为英文注释。
- 当前问题清单写入 `questions.md`。

### 本轮验证

- `blog` Maven 编译已通过：`mvn -pl boylu-server -am package -DskipTests`。
- `blog-web` 构建已通过：`npm run build`。
- `blog-admin` 构建已通过：`npm run build`。
- 前台构建保留 `mavon-editor` eval 警告；后台构建保留 Sass legacy API 和大 chunk 警告。
- 依赖安装阶段保留 npm audit 提示，后续可单独做依赖安全升级。

## 待关注的问题与风险

### 图片处理内存

- 普通小文件上传仍保留现有 `MultipartFile#getBytes()` 流程，受上传大小和像素尺寸限制保护。
- 文章封面变体生成仍需要解码图片；如后续图片量或并发变大，建议把封面生成改为异步队列并继续降低峰值内存。

### 前端构建警告

- `blog-web` 构建仍可能出现 `mavon-editor` 依赖自身的 eval 警告。
- `blog-admin` 构建仍可能出现 Sass legacy API 和大 chunk 警告。
- 当前不影响构建产物，可后续单独做依赖拆分和升级。

## 验证要求

- `blog-web` 执行 `npm run build`。
- `blog-admin` 执行 `npm run build`。
- `blog` 执行 Maven 编译，至少验证 `boylu-server` 及依赖模块。
- 部署后检查：
  - `sudo systemctl status boylu-blog --no-pager`
  - `sudo nginx -t`
  - 首页 `全部` 分类、相册页、文章详情页、留言板。
  - `/boylu/api/article/home-list?pageNum=1&pageSize=10` 连续请求不返回 500。
  - `/boylu/file/content/{id}` 能访问并最终命中 `/localFile/...`。
  - `/localFile/...` 图片能由 Nginx 直接返回。
  - `/img/local/<encoded>!w320.webp` 能返回样式图或正确回退。

## 工作偏好

- 回答用中文。
- 少说空话，先给结果，再解释。
- 优先复用现有项目结构，不要随意推翻重做。
- 博客页面优化重点是简洁、现代、留白、视觉层级和响应式。
- 不要擅自删除核心功能，不要为了“高级感”引入复杂依赖。
