# 项目问题清单与当前状态

更新时间：2026-05-19

## 当前仓库状态

- 本轮 Notion 图片本地化队列、失败重试、服务重启恢复、Notion 读取兜底和生产日志降噪已完成代码侧修复。
- 当前代码已部署到线上并完成真实 Notion 导入/同步验证。
- `blog-admin/src/components.d.ts` 仍显示修改但无内容 diff，疑似生成文件换行状态；不纳入本轮提交。

## P2：文章封面上传

- 当前处理：
  - 上传组件成功后强制切换为 `success`，避免 100% 圆圈卡住。
  - 文章封面上传改为先保存原图并立即返回，不再同步生成多尺寸封面。
  - 后台文章编辑弹窗新增“封面上传回执”，可直接看到当前返回地址、尺寸和回退地址，便于线上验收 canonical 返回值是否正确。
- 验证状态：
  - 后台已部署新版文章编辑页，封面上传回执可用于线上继续观察 canonical 地址。
  - 仍需后续在真实编辑文章时复测一次：上传封面后卡片立即显示图片，保存后前台首页和文章详情显示新封面。
  - 上传返回值仍需后续单独确认保持 `/boylu/file/content/{fileId}` 风格，而不是回退成 `/localFile/...` 直链。

## P2：Notion 图片本地化

- 当前处理：
  - 导入时可先保留原链接。
  - 后台异步下载图片到本站本地。
  - 同步日志会记录图片状态、总图片数、成功数、失败数。
  - 后台文章页新增“图片队列”入口，单独查看 `pending/running/partial_failed/failed` 图片本地化任务。
  - 队列视图支持按图片状态筛选、按标题/来源/提示词搜索，并在存在活动任务时自动刷新。
  - Notion 导入/同步返回结果补充 `logId`，后续前端或排查脚本可以直接关联到具体日志。
  - 后端新增持久化队列恢复任务：服务启动后会定时扫描 `pending/running` 图片日志并继续处理，不再只依赖当次 JVM 内存线程。
  - 后台 Notion 日志卡片新增“重试图片”按钮，失败或部分失败任务可直接重新入队。
  - 手动重试时会先重新读取 Notion 页面，刷新正文和封面里仍保留的远程临时图链，再执行本地化。
  - 队列恢复任务对“旧的 pending/running 日志”会按更新时间判断是否需要先刷新最新 Notion 临时图链，避免服务重启后拿过期链接继续下载。
  - 已修正文中“部分图片已本地化、部分仍是外链”时的临时图链刷新错位问题，改为按全文图片位置对齐刷新，避免把新临时链接替换到错误图片上。
  - Notion 页面 ID 提取逻辑已修复，`MISC-ad003e...` 这类 slug 不会再把 slug 末尾十六进制字符拼进 Page ID。
  - Notion 读取增加 `curl` 最终兜底：Java/Hutool 连接失败时走服务器已验证可通的系统 curl，请求 token 写入临时配置文件并在请求后删除，避免出现在进程参数中。
  - curl 兜底解析已兼容“正文是有效 JSON 但状态码未带回”的异常输出，避免成功响应被误判失败。
- 线上验证：
  - MISC Notion 页面已真实导入成功：`articleId=393`、`logId=15`、导入块数 437。
  - 首次图片本地化已完成：7/7 成功。
  - 已做可恢复故障注入：把新导入草稿第一张图临时改成失效远程 URL 后，后台“重试图片”接口可先刷新 Notion 临时图链，再恢复为 `/boylu/file/content/...`，最终 1/1 成功。
  - 已验证服务重启恢复：遗留 `running` 图片任务在 `boylu-blog` 重启后被调度器捞起，刷新临时图链后最终 1/1 成功。
  - 最终部署版本再次同步 `articleId=393` 返回 `skipped`，说明线上 jar 可正常读取 Notion 元数据，并在无变化时跳过更新。
- 剩余风险：
  - 图片本地化仍是单机进程内消费模型，不是独立消息队列；极端高并发下仍不适合当作分布式任务系统使用。
  - Notion 原文结构如果发生大幅改写，且当前站内文章已经人工插入/删除图片，按位置刷新临时图链仍可能需要人工复核个别图片对应关系。
  - 失败任务重试、服务重启恢复、临时图链刷新三条链路已通过真实线上验证；后续主要观察极端网络抖动和大图下载耗时。

## P2：生产日志过于 verbose

- 现象：线上日志曾大量输出 MyBatis SQL、用户记录、文章内容片段。
- 当前处理：
  - `application.yml` 已去掉全局 `StdOutImpl`，避免所有环境默认打印 MyBatis SQL。
  - `application-dev.yml` 单独保留开发环境 SQL stdout 和 mapper refresh。
  - `application-prod.yml` 新增 `root/com.boylu=INFO`、`org.apache.ibatis/com.baomidou/org.mybatis/org.springframework.jdbc=WARN`，用于压低生产日志噪音。
- 线上验证：
  - 新版后端已部署并重启，`boylu-blog` 服务处于 `active`。
  - 重启后的 `prod` 日志未再命中 MyBatis `Preparing`、`Row`、`Total` 明细。
  - 前台入口、后台入口和公开 API 均已返回 200。
- 剩余风险：
  - 仍需继续观察是否还有文章正文、用户记录等过长业务日志；如果出现，再做点位级裁剪。

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

## 已处理记录

- 2026-05-19 CodeRabbit 仓库审查配置：
  - 新增仓库根目录 `.coderabbit.yaml`，配置 CodeRabbit 以中文输出、自动审查发往 `main` 的 PR，并开启增量审查。
  - 按项目结构补充前台 `blog-web`、后台 `blog-admin`、后端 `blog/boylu-*`、部署脚本和文档/SQL 的路径级审查重点。
  - 排除 `node_modules`、`dist`、`target`、`.tmp`、source map、压缩文件和 `blog-admin/src/components.d.ts` 等噪声文件，减少无效审查评论。
  - 启用 GitHub Checks、ESLint、Stylelint、PMD、Semgrep/OpenGrep、密钥扫描、ShellCheck、actionlint 和 yamllint；关闭 markdownlint 与 LanguageTool，避免中文文档风格类噪声。
  - 已创建 GitHub PR #2 并触发 CodeRabbit；首轮审查指出的移动端侧边栏初始布局闪烁和 `scheduleIdleTask` SSR 兜底问题已修复。
  - 修复后 `blog-web npm run build` 通过，并已同步最新前台构建产物到线上服务器；`nginx -t`、首页和首页文章接口检查均通过。
- 2026-05-19 前台轻量 UI 与等待策略优化：
  - `App.vue` 站点配置请求增加 900ms 首屏兜底，避免配置接口慢时启动骨架长时间占位；通知请求改为空闲期再拉取。
  - 首页文章分页滚动补齐 `postsSection` 锚点，避免分页时滚动目标为 `NaN`。
  - 首页文章卡片减少 `transition: all`，改为只过渡 transform、shadow、border-color；同时微调边框、阴影、行高和移动端内边距。
  - 第二轮继续优化首页首屏：桌面端侧边栏改为空闲后挂载，移动端不再创建隐藏侧边栏，减少推荐文章、标签云和状态面板的首屏并发请求与动画负担。
  - 最新说说组件增加空数据/单条数据保护，避免无内容时仍启动轮播定时器；同时轻量调整容器边框、阴影和文本 hover 过渡。
  - 本轮没有引入 HyperFrames 到主工程；HyperFrames 仅适合后续制作博客宣传视频或文章转视频。
- 2026-05-18 线上部署与 Notion 队列验证：
  - 已备份线上后端 jar、前台静态文件和后台静态文件后替换当前构建产物。
  - Windows `Compress-Archive` 产物在 Linux `unzip` 下存在反斜杠路径问题，已改用 `tar.gz` 重新打包前后台静态产物完成部署。
  - 后端新版 jar 已部署并重启，前台、后台入口和公开 API 验证为 200。
  - 已确认生产日志不再持续输出 MyBatis SQL 明细。
  - 已完成 MISC Notion 真实导入、图片本地化、失败重试、服务重启恢复和最终 `skipped` 同步验证。
- Notion 图片本地化队列与排查：
  - 已新增后台“图片队列”入口，面向活动任务和失败任务集中排查。
  - 已支持队列摘要、图片状态筛选、关键字搜索和活动任务自动刷新。
- Notion 自动同步保护：
  - 已防止空正文、空摘要、空封面等空值覆盖旧文章。
  - 无有效差异时记录 `skipped`，不更新正文、摘要、封面，也不排队图片本地化。
- Notion 页面读取连接拒绝：
  - 已增加直连 + 备用请求、超时、重试、非重试 HTTP 状态识别。
- 百度推送 SSL 握手错误：
  - 默认改为 HTTP 端点，保留 HTTPS/HTTP 候选端点重试；失败不再误标记已推送。
- `ip2region.xdb` 读取权限异常：
  - 已改为优先从 classpath 直接读入内存，不再依赖当前工作目录读写权限。
- 图片每次进入页面加载慢：
  - 已增加 Service Worker cache-first、长缓存、文件网关 Redis 缓存和样式图命中缓存快速返回。
- Notion 同步日志可视化与删除：
  - 已在后台文章管理页增加日志查看、刷新、单条删除、批量删除和可见日志删除。
- 相册与文章图片缺失修复：
  - `2025年10月` 相册封面和 12 张照片已恢复。
  - 全部相册与照片引用扫描结果已修到 `issue_count = 0`。
  - 全部文章共扫描 80 篇、417 个内部图片引用，最终内部图片引用扫描 `issue_count = 0`。
  - 无法找回原图的旧图片已替换为明确缺失占位图 `/localFile/local-plus/system/missing-image.svg`。

## 最近验证结果

- 2026-05-19 前台轻量优化验证：
  - `blog-web npm run build` 已通过，并同步更新 `blog-web/public/sitemap.xml` 的 `lastmod` 到 2026-05-19。
  - `blog-admin npm run build` 已通过；仍保留已知 Sass legacy API 和大 chunk 警告。
  - `blog` Maven 编译已通过：显式设置 `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_311` 后执行 `mvn -pl boylu-server -am package -DskipTests`。
  - 第二轮优化后再次执行 `blog-web npm run build`、`blog-admin npm run build`、`blog` Maven 编译，均已通过；`git diff --check` 无格式错误，仅保留 Windows 换行提示。
- 后端 Maven 编译已通过：`mvn -pl boylu-server -am package -DskipTests`。
- 前台构建已通过：`blog-web npm run build`。
- 后台构建已通过：`blog-admin npm run build`。
- 线上三份包 SHA256 已和本地一致后再执行替换部署。
- 线上 `boylu-blog` 重启后处于 active。
- 线上 `prod` 日志已确认不再输出 MyBatis SQL 明细。
- 线上公开接口和前后台入口已确认返回 200。
- 线上真实 Notion 导入已通过：`articleId=393`、`logId=15`、图片本地化 7/7 成功。
- 线上图片失败重试已通过：失效远程图刷新后恢复为 `/boylu/file/content/...`。
- 线上服务重启恢复已通过：遗留 `running` 图片任务重启后最终 1/1 成功。
- 最终线上 jar 对 `articleId=393` 再次同步返回 `skipped`，验证 Notion 元数据读取和无变化跳过逻辑正常。
- 已补做代码侧校验：队列恢复会优先处理旧 `running/pending` 任务，失败任务支持后台直接重试，重试前会刷新最新 Notion 临时图链。
- 这次本地 Maven 需要显式设置 `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_311` 后才可运行；不是代码错误，是当前终端环境变量缺失。
- 已确认 `boylu-blog.jar` 内包含 `BOOT-INF/classes/ip2region.xdb`。
- 提交前敏感信息扫描未发现真实 Notion token、服务器密码或硬编码真实密钥；命中的均为环境变量占位或代码变量。
- `git diff --check` 当前无格式错误，仅有 Windows 换行提示。

## 下一步优先级

1. 后台编辑一篇文章，实测封面上传回执、保存后前台首页/详情封面展示是否正常。
2. 用浏览器复测首页、文章页、相册页图片二次加载速度。
3. 手动推送一篇文章到百度，确认成功/失败结果真实。
4. 继续处理 P2：公开仓库脱敏、点位级业务日志裁剪。

## 记录规则

- 后续每次修复完问题后，都要同步更新本文件。
- 已修复的问题从活动问题清单删除，只保留必要的已处理记录。
- 未完全解决的问题继续保留优先级，避免修完代码但问题台账滞后。
