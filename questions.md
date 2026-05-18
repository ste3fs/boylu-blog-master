# 项目问题清单与当前状态

更新时间：2026-05-18

## 当前仓库状态

- 当前工作树仍有未提交改动，主要集中在 Notion 同步、百度推送、图片缓存、`ip2region.xdb` 读取和文档记录。
- 当前修改文件包括：
  - `AGENTS.md`
  - `questions.md`
  - `blog-admin/src/views/article/article/index.vue`
  - `blog-web/public/sw.js`
  - `blog-web/public/sitemap.xml`
  - `blog/boylu-admin/src/main/java/com/boylu/service/impl/SysArticleServiceImpl.java`
  - `blog/boylu-admin/src/main/java/com/boylu/service/notion/NotionArticleSyncJob.java`
  - `blog/boylu-admin/src/main/java/com/boylu/service/notion/NotionImportService.java`
  - `blog/boylu-admin/src/main/java/com/boylu/service/notion/NotionSyncLogService.java`
  - `blog/boylu-admin/src/main/java/com/boylu/service/seo/BaiduPushService.java`
  - `blog/boylu-commom/src/main/java/com/boylu/config/WebMvcConfig.java`
  - `blog/boylu-commom/src/main/java/com/boylu/utils/IpUtil.java`
  - `blog/boylu-file/src/main/java/com/boylu/controller/FileController.java`
  - `blog/boylu-file/src/main/java/com/boylu/controller/LocalImageStyleController.java`
  - `blog/boylu-server/src/main/resources/application-dev.yml`
  - `blog/boylu-server/src/main/resources/application-prod.yml`
- `blog-admin/src/components.d.ts` 在 `git status` 中显示修改，但当前没有内容 diff，提交前需要再确认是否为生成文件/换行导致。
- `blog-web/public/sitemap.xml` 是前台构建自动更新，主要把 `lastmod` 更新为 `2026-05-18`。

## P1：Notion 页面读取连接拒绝

- 原问题：服务器曾出现 `Notion 页面读取失败：Connection refused`，连续重试后导入失败。
- 当前处理：
  - Notion 请求链路保留 JDK `Proxy.NO_PROXY` 直连 + Hutool 备用请求。
  - 默认超时提高到 20 秒。
  - 默认重试次数提高到 6 次。
  - 429、5xx、超时、连接拒绝会继续重试。
  - 400、401、403、404 等明确权限或地址错误直接失败。
  - 新增配置项：`NOTION_API_BASE`、`NOTION_REQUEST_TIMEOUT_MS`、`NOTION_REQUEST_RETRY_TIMES`、`NOTION_REQUEST_RETRY_DELAY_MS`。
- 剩余风险：
  - Notion 外部网络仍可能偶发失败，但失败不应再破坏已有文章。
- 线上验证：
  - 使用真实 Notion 链接导入一次。
  - 查看同步日志里失败原因是否清晰，不应只显示大段堆栈。

## P1：Baidu 推送 SSL 握手错误

- 原问题：线上日志出现 `No subject alternative DNS name matching data.zz.baidu.com found`，百度主动推送失败。
- 当前处理：
  - 百度推送默认端点改为 `http://data.zz.baidu.com/urls`，避开服务器侧 HTTPS 证书握手异常。
  - 推送会按顺序尝试配置端点、HTTPS 默认端点、HTTP 降级端点。
  - 每个端点默认重试 2 次，并设置连接/读取超时。
  - 百度返回 `error`、HTTP 4xx/5xx 或网络异常时返回失败。
  - 推送失败不再把文章标记为 `is_baidu_pushed=1`。
  - 后台手动推送按钮现在会显示真实成功或失败。
- 剩余风险：
  - 如果百度 token、站点域名或百度站长平台绑定关系错误，接口仍会失败，需要修正配置。
- 线上验证：
  - 后台单篇文章点击“推送百度”，成功时显示成功，失败时显示失败。
  - 检查数据库 `is_baidu_pushed`，失败时不能被改成 1。

## P1：`ip2region.xdb` 读取权限异常

- 原问题：服务器日志出现 `failed to load content from ip2region.xdb: AccessDeniedException: ip2region.xdb`。
- 当前处理：
  - `IpUtil` 不再把 classpath 中的 `ip2region.xdb` 写到当前工作目录再读取。
  - 现在优先从 classpath 直接读入内存。
  - 支持 `-Dip2region.xdb.path=/path/to/ip2region.xdb` 或 `IP2REGION_XDB_PATH` 指定外部文件。
  - 加载失败时返回 `UNKNOWN`，不影响主业务流程。
  - 已确认打包后的 `boylu-blog.jar` 内包含 `BOOT-INF/classes/ip2region.xdb`。
- 剩余风险：
  - 需要部署后确认线上启动日志不再出现当前目录 `ip2region.xdb` 的 `AccessDeniedException`。

## P1：图片每次进入页面加载慢

- 原问题：文章、首页、相册图片已经接入样式图，但每次进入页面仍可能重新加载很久。
- 当前处理：
  - 前台 Service Worker 升级为 v2。
  - `/localFile/`、`/img/local/`、文件网关图片、默认头像和 logo 改为 cache-first。
  - `/localFile/...` 静态资源缓存从 30 天改为 365 天。
  - 文件网关解析真实本地文件地址后的 Redis 缓存从 1 天延长到 30 天。
  - 样式图接口命中已有 `/localFile/img-cache/...` 时直接重定向，不再每次读取图片元数据或解码源图。
  - 样式图缓存名改为基于源文件路径、大小、修改时间生成，不再为了计算 hash 完整读取源图内容。
- 剩余风险：
  - 首次生成某个宽度/格式的样式图仍需要解码源图，第一次访问可能仍慢。
  - 如果线上 Nginx 覆盖了 `/localFile/` 缓存头，浏览器缓存效果会打折。
  - Service Worker 更新后，用户浏览器需要重新激活 v2，旧缓存不会立即全部失效。
- 线上验证：
  - 首次打开文章页后刷新，图片应明显更快。
  - 检查 `/localFile/...` 响应头是否有长缓存。
  - 检查 `/img/local/<encoded>!w320.webp` 第二次请求是否直接命中缓存图。

## P1：Notion 同步日志可视化与删除

- 原问题：导入失败只弹错或写服务器日志，后台看不清哪篇成功、失败、跳过、失败原因和图片本地化进度，也不能删除日志。
- 当前处理：
  - 后台文章列表已有“Notion 日志”入口。
  - 日志弹窗改为卡片式展示，同步状态支持 `running`、`success`、`failed`、`skipped`。
  - 日志中展示文章、来源地址、同步状态、图片状态、导入块数、变更字段数、图片数量、消息、警告和错误详情。
  - 支持刷新日志。
  - 支持删除单条日志、删除选中日志、删除当前可见日志。
- 剩余风险：
  - 仍不是独立的“同步任务中心”，只能从文章管理页查看最近或单篇文章日志。
  - 需要线上浏览器复测删除权限、移动端弹窗宽度和长错误文本折行。

## P2：文章封面上传

- 当前处理：
  - 上传组件成功后强制切换为 `success`，避免 100% 圆圈卡住。
  - 文章封面上传改为先保存原图并立即返回，不再同步生成多尺寸封面。
- 剩余验证：
  - 后台编辑文章上传封面后，封面卡片应立即显示图片。
  - 保存文章后，前台首页和文章详情应显示新封面。

## P2：Notion 图片本地化

- 当前处理：
  - 导入时可先保留原链接。
  - 后台异步下载图片到本站本地。
  - 同步日志会记录图片状态、总图片数、成功数、失败数。
- 剩余风险：
  - 后台还没有专门的图片本地化队列页面。
  - 某张图失败时，目前主要通过 Notion 日志查看，不适合批量排查大量图片。
  - Notion 临时图片链接可能过期，长时间后再下载仍可能失败。

## P2：生产日志过于 verbose

- 现象：线上日志曾大量输出 MyBatis SQL、用户记录、文章内容片段。
- 风险：
  - 日志体积变大。
  - 服务器日志中可能出现手机号、邮箱、文章正文等敏感或半敏感信息。
- 建议：
  - 生产环境关闭 MyBatis SQL 明细日志。
  - 只保留必要错误、慢接口和关键业务日志。

## P2：公开仓库信息暴露

- 现象：公开文档和配置中仍可能出现真实服务器路径、后台入口示例、历史 SQL 等信息。
- 风险：
  - 这些不一定是密码，但会降低隐藏后台入口和部署细节的保护效果。
  - 历史 SQL 可能包含不适合公开的数据结构或初始化账号信息。
- 建议：
  - README 只保留脱敏部署说明。
  - 后台入口使用 `/admin-secret-path/` 这类示例。
  - 检查 SQL 是否包含真实数据，必要时替换为脱敏初始化脚本。

## P3：前端构建警告

- 现象：
  - 前台构建存在 `mavon-editor` 自身 eval 警告。
  - 后台构建存在 Sass legacy API 和大 chunk 警告。
- 影响：
  - 当前不影响构建产物。
- 建议：
  - 后续单独做依赖拆分和 Sass API 升级。

## P3：历史目录与生成文件

- 现象：
  - 仓库仍可能存在旧项目目录、历史 SQL、生成文件修改状态。
  - `blog-admin/src/components.d.ts` 当前显示修改但没有内容 diff。
- 建议：
  - 提交前确认生成文件是否需要纳入提交。
  - 继续保持当前有效目录：`blog-web`、`blog-admin`、`blog/boylu-*`。

## 已处理：相册与文章图片缺失修复

- `2025年10月` 相册封面和 12 张照片已恢复。
- 全部相册与照片引用扫描结果已修到 `issue_count = 0`。
- 全部文章共扫描 80 篇、417 个内部图片引用，最终内部图片引用扫描 `issue_count = 0`。
- 无法找回原图的旧图片已替换为明确缺失占位图 `/localFile/local-plus/system/missing-image.svg`。
- 不删除旧存储文件，不用其它无关图片冒充缺失图片。

## 最近验证结果

- 后端 Maven 编译已通过：`mvn -pl boylu-server -am package -DskipTests`。
- 前台构建已通过：`blog-web npm run build`。
- 已确认 `boylu-blog.jar` 内包含 `BOOT-INF/classes/ip2region.xdb`。
- `git diff --check` 当前无格式错误，仅有 Windows 换行提示。

## 下一步优先级

1. 部署当前后端和前台到服务器。
2. 重启 `boylu-blog`，检查启动日志里是否还有 `ip2region.xdb AccessDeniedException`。
3. 用浏览器复测首页、文章页、相册页图片二次加载速度。
4. 手动推送一篇文章到百度，确认成功/失败结果真实。
5. 检查 Notion 日志弹窗：状态展示、错误折行、删除功能。

## 记录规则

- 后续每次修复完问题后，都要同步更新本文件。
- 已修复的问题需要标明“当前处理/已处理”、剩余风险和验证结果。
- 未完全解决的问题继续保留优先级，避免修完代码但问题台账滞后。
