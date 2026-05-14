# AGENTS.md

## 项目信息

- 项目名称：boylu 个人博客
- 本地路径：`E:\做题\shiyi-blog-master`
- 线上服务器：`111.229.123.234`
- SSH 用户：`ubuntu`
- 前台站点目录：`/var/www/boylu-blog/`
- 后台管理目录：`/var/www/boylu-blog/admin/`
- 后端服务目录：`/opt/boylu-blog/server/`
- 上传文件目录：`/opt/boylu-blog/storage/`
- 后端服务名：`boylu-blog`
- Nginx 站点配置：`/etc/nginx/sites-enabled/boylu-blog.conf`
- Naxsi 日志：`/var/log/nginx/naxsi_public.log`
- Naxsi 日报脚本：`/usr/local/bin/naxsi_daily_report.py`

## 当前有效代码目录

- 前台前端：`blog-web`
- 后台前端：`blog-admin`
- 后端 Maven 工程：`blog/boylu-*`
- 当前有效后端入口：`blog/boylu-server`
- 旧的 `blog/mojian-*` 目录不是当前修改目标，不要恢复或混用。

## 部署方式

- 前台构建：在 `blog-web` 执行 `npm run build`
- 后台构建：在 `blog-admin` 执行 `npm run build`
- 后端构建：在 `blog` 下执行 `E:\做题\shiyi-blog-master\.tools\apache-maven-3.9.9\bin\mvn.cmd -pl boylu-server -am package -DskipTests`
- 前台构建产物同步到：`/var/www/boylu-blog/`
- 后端 jar 同步到：`/opt/boylu-blog/server/boylu-blog.jar`
- 更新后重启服务：`sudo systemctl restart boylu-blog`
- 重载 Nginx 前必须先执行：`sudo nginx -t`

## 图片和旧数据规则

- 不删除、不移动、不覆盖 `/opt/boylu-blog/storage/` 中的旧图片。
- 必须继续兼容旧图片地址：
  - `/localFile/...`
  - `/file/content/{id}`
  - `/file/view/{id}`
  - `/boylu/file/content/{id}`
  - `/mojian/file/content/{id}`
- 本地文件优先通过 `/localFile/...` 让 Nginx 静态缓存返回。
- 新增本地淘宝/OSS 类图片样式读取：`/img/local/<base64url旧地址>!w320.webp`，首次请求由后端生成 `/localFile/img-cache/...` 缓存图，后续由 Nginx 静态缓存返回。
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
- 后端保留原 CoderUtil 逻辑，密钥缺失、请求失败或返回空数据时切换到免密兜底源。
- 覆盖 `weibo`、`zhihu`、`toutiao`、`baidu`、`csdn` 五个热榜类型。

### 首页体验修复

- 修复首页 `全部`分类偶发卡住：Redis 首页缓存中的 `records` 会被反序列化成 `JSONObject`，现在会显式转换为 `HomeArticleVo`，失败时删除缓存并回源数据库。
- 修复快速切换分类时旧请求覆盖新请求的问题，首页只接收最后一次文章列表请求结果。
- 修复图片灰块：`SmartImage` 响应式图片失败后先回退旧封面/原图地址，再回退默认图。
- 首页阅读时间按旧 GitHub 版本逻辑估算，前端不再优先相信后端由摘要估算的 `readingTime`。
- 删除右下角“AI 提问”悬浮框；顶部导航里的“AI 助手”入口保留。
- 今日状态恢复为“访客 / 浏览”，读取总访客和总浏览，不再显示“今日访客 / 今日浏览”。

### 2026-05-01 首页统计和阅读量修复

- 首页阅读时间改为后端按文章正文 `content_md/content/summary` 估算，前端优先展示后端 `readingTime`。
- 文章详情阅读量改为每次打开详情页即时 +1，不再按同一访客同一文章每天只加一次。
- 首页“今日状态”第一格显示“今日访客”，第二格显示“总访客”。
- Naxsi 日报脚本改为同时读取当前日志和轮转日志，避免日志轮转后邮件统计为 0。

### Naxsi 日报说明

- Naxsi 没失效：服务器 `/var/log/nginx/naxsi_public.log` 仍有 `NAXSI_FMT` 拦截记录，Fail2ban 也有累计处理记录。
- 邮件里某几天显示 0，是因为定时任务每天 00:15 统计“昨天”的拦截，4 月 26-28 日没有被脚本统计到的拦截。
- 日报脚本需要保留昨日统计，同时增加“今日截至当前拦截数”和“最近一条拦截时间”，避免误判防护失效。

### 淘宝式图片加载和表格日报

- 后端新增 `/img/local/{encoded}!w{width}.{format}` 图片样式接口，支持 `160/240/320/480/640/960/1280` 宽度和 `webp/jpg` 输出。
- 后端登录拦截白名单已放行 `/img/**`，图片样式读取不要求登录。
- 图片样式接口只读取 `/localFile/...`、`/file/content/{id}`、`/file/view/{id}`、`/boylu/file/content/{id}`、`/mojian/file/content/{id}` 等旧地址，不修改旧 URL 和数据库原字段。
- 前端 `SmartImage` 默认优先请求 `320/480/640` WebP 小图，失败时回退旧原图，再回退默认图。
- 首页文章列表第一页前两张图设置高优先级，其余图片懒加载。
- 文章详情富文本图片渲染时动态改为小 WebP 地址，同时保留 `data-origin` 原图用于预览和失败回退。
- 首页文章列表阅读时间不再使用后端旧 `readingTime` 字段，按当前摘要/正文重新估算，列表摘要模式显示更接近旧版的 1 分钟阅读。
- Nginx 增加 `/img/` 代理到后端生成入口；生成后的 `/localFile/img-cache/...` 继续由 Nginx 静态缓存一年。
- Naxsi 日报脚本本地模板在 `deploy/security/`，部署到 `/usr/local/bin/`；每日生成 `.txt` 和 `.html` 两份报告。
- Naxsi 邮件改为 `multipart/alternative`，QQ 邮箱优先展示 HTML 表格，包含总览、Top IP、规则类型、命中分值桶、被扫路径、今日实时状态、Fail2ban 状态和读取日志文件。

### 图片模糊、阅读时间和 QQ 邮件适配修复

- `SmartImage` 不再因为父组件重新创建 `image` 对象就重置加载状态，只有最终图片 URL 变化才重置。
- 图片加载成功后会在浏览器 `localStorage` 写入 20 天加载状态缓存；再次打开或刷新时命中缓存就直接显示清晰图，不再先显示 blur 占位。
- 首页阅读时间恢复优先使用后端 `readingTime` 字段；字段缺失时才用前端摘要兜底估算。
- Naxsi HTML 日报加 `viewport`，Top 攻击 IP 从 7 列宽表改为手机友好的卡片式字段展示，避免 QQ 手机端一字一列竖排。
- 邮件发送脚本 `naxsi_daily_mail.py` 已改为 Base64 UTF-8 正文和编码后的标题，避免 QQ 邮箱把中文报告显示成问号；必要时报告 HTML 可把中文转成数字实体再发送。

### 2026-05-02 个人中心功能修复

- 个人资料表单恢复真实校验：昵称字段走 `profileForm.nickname` 规则，保存前先执行 Element 表单校验。
- 个人资料保存只提交昵称、邮箱、简介、性别、头像等允许字段；后端忽略前端传入的用户 ID，统一按当前登录用户更新。
- 重置按钮恢复为“回到本次加载的用户资料”，不再把表单清空。
- 头像上传成功后同步更新个人中心表单、侧边资料卡和 Vuex 用户信息。
- 账号绑定页移除前端假绑定数据，不再显示 `wx_user123`、`github_user` 这类模拟账号；当前后端没有安全绑定接口时只提示暂不支持，不再伪造成功状态。
- 个人中心修改密码接口取消后台权限码限制，只要求登录并校验旧密码；后端增加旧密码/新密码空值和新密码长度校验。
- 个人中心意见反馈允许普通登录用户提交；反馈列表按真实登录角色判断是否管理员，普通用户只能看自己的反馈，不能通过请求参数伪造管理员视图。
- 个人中心“我的评论/我的回复”内容渲染增加 HTML 清洗，避免评论内容通过个人中心产生脚本注入。
- 删除我的评论时后端会校验评论属于当前登录用户，不能通过构造 ID 删除别人的评论；删除自己的父评论时仍会清理它下面的回复。

### 2026-05-14 项目巡检和进度同步

- 用户提到的 `angents.md` 按当前仓库实际文件 `AGENTS.md` 处理。
- 当前有效源码继续以 `blog-web`、`blog-admin`、`blog/boylu-*` 为准，后端 Java 包名已切换为 `com.boylu`；旧 `blog/mojian-*` 不再作为修改目标。
- 当前本地 Git 工作树仍处于未整理状态：旧 `blog/mojian-*` 在 Git 中表现为删除，新 `blog/boylu-*` 目录尚未被 Git 跟踪；后续提交前必须先确认改名迁移范围。
- 已有性能优化进度：文章详情短 TTL 缓存、ETag、索引优化、详情页组件懒加载、Markdown 工具懒加载、上传分片接口、上传失败率/详情加载耗时上报已落地。
- 已有 SEO 进度：`robots.txt`、`sitemap.xml` 生成脚本、页面级 meta/canonical、文章详情结构化数据和百度主动推送接口已加入项目。
- 图片链路当前状态：前端 `SmartImage.vue` 和文章详情正文图片已接入 `/img/local/<encoded>!w{width}.{format}` 样式图链路；后端 `LocalImageStyleController` 和 Nginx `/img/` 代理继续保留旧 URL 兼容。
- 当前问题清单写入 `questions.md`，已按 2026-05-14 修复进度更新；后续优先处理 Git 工作树整理、图片 canonical 策略和文档一致性。
- 根目录 `deploy_to_server.py` 和 `deploy_web_to_server.py` 已移除硬编码服务器密码，改为读取环境变量 `BOYLU_SSH_PASSWORD` 或使用 SSH key / ssh-agent。
- 不要把服务器密码、数据库密码、Token 写入 `AGENTS.md`、`questions.md`、脚本或提交记录；服务器登录建议改用 SSH key 或临时授权。

### 2026-05-14 博客问题修复进度

- `SmartImage.vue` 已实现样式图 srcset/default 生成，优先请求 320/480/640 WebP，小图失败后回退原图和默认图。
- 文章详情正文图片懒加载已接入样式图，保留 `data-origin` 原图用于预览和失败回退。
- `blog-web` 和 `blog-admin` 本地开发代理已补齐 `/img`。
- 后端热榜接口已增加 Redis 缓存，减少第三方接口慢响应和限流风险。
- 后台文章保存已接入 `HtmlSanitizerUtil` 做文章 HTML 清洗。
- `LocalImageStyleController` 已避免为 hash 一次性读取原图，增加源图大小/像素保护、GIF 回退、透明 PNG 转 JPG 回退、WebP 保留 alpha 和同一缓存文件生成锁。
- `FileController` 已增加 `img-cache` 统计和安全清理接口，只清理缓存图，不处理旧原图。
- `SaTokenConfigure` 已移除全局 `/api/**` 放行，改为精确公开接口白名单。
- 后端 Maven 编译已通过：`mvn -pl boylu-server -am package -DskipTests`。
- 前台 `blog-web` 和后台 `blog-admin` 构建已通过：`npm run build`。

## 待修复的问题与风险

### Git 状态整理
- 当前有效后端 `blog/boylu-*` 仍是未跟踪目录，旧 `blog/mojian-*` 是 Git 中的删除状态；正式提交前需要把改名迁移整理为一次清晰变更，避免仓库不可复现。
- `.tmp/` 目前包含较多临时数据库探测、修复和部署脚本，建议加入 `.gitignore` 或清理，只保留可复用脚本到 `scripts/`、`deploy/`。

### 图片链路一致性
- `docs/IMAGE_STORAGE.md` 写业务字段统一保存 `/boylu/file/content/{fileId}`，但当前上传接口在本地存储场景会直接返回 `/localFile/...`；需要统一“长期保存网关地址”还是“保存本地静态地址优先性能”的策略。
- `docs/IMAGE_STORAGE.md` 写 `/localFile/` 缓存 30 天，但 Nginx 当前配置是 365 天 immutable 缓存；文档需要同步真实部署策略。

### 性能优化
- 图片处理已增加基础保护和降级；如果后续上传图片量和并发继续增大，再把 `ArticleCoverImageService` 与分片完成后的上传流程改成更完整的流式处理。

### 安全增强
- 服务器密码曾出现在部署脚本中，虽然本次已改为环境变量读取，但如果旧脚本曾提交或传播，仍建议更换服务器密码并改用 SSH key。

### 内存与稳定性
- 分片上传完成后会把合并后的文件再次 `Files.readAllBytes` 到内存，再走图片解析和上传；大图或并发上传时仍有内存峰值风险。
- 开发环境 `application-dev.yml` 中 AI 推理接入点等关键配置缺失，需手动配置后方可本地调试 AI 相关功能。

## 验证要求

- `blog-web` 执行 `npm run build`。
- `blog` 执行 Maven 编译，至少验证 `boylu-server` 及依赖模块。
- 部署后检查：
  - `sudo systemctl status boylu-blog --no-pager`
  - `sudo nginx -t`
  - 首页 `全部`分类、相册页、文章详情页、留言板。
  - `/boylu/api/article/home-list?pageNum=1&pageSize=10` 连续请求不返回 500。
  - `/localFile/...` 图片能由 Nginx 直接返回。
  - `/img/local/<encoded>!w320.webp` 能返回 302，并最终落到 `/localFile/img-cache/...`。

## 工作偏好

- 回答用中文。
- 少说空话，先给结果，再解释。
- 优先复用现有项目结构，不要随意推翻重做。
- 博客页面优化重点是简洁、现代、留白、视觉层级和响应式。
- 不要擅自删除核心功能，不要为了“高级感”引入复杂依赖。
