# 图片存储与加载约定

## 最终策略

业务字段、文章内容、相册、头像和封面元数据中，长期保存的 canonical 图片地址统一使用文件网关地址：

```text
/boylu/file/content/{fileId}
```

这个地址只依赖数据库里的 `file_detail.id`，不绑定服务器 IP、域名、磁盘真实路径或当前存储平台。以后迁移服务器时，只需要迁移数据库、`/opt/boylu-blog/storage/` 文件目录和 Nginx 配置，不需要批量修改业务表里的图片地址。

运行时仍保留性能优先策略：浏览器访问 `/boylu/file/content/{fileId}` 后，后端查出真实文件地址并跳转到 `/localFile/...` 或外部对象存储地址；本地文件最终由 Nginx 静态缓存返回。

## 当前链路

1. 后台上传组件调用 `/file/upload?source=xxx` 或分片上传接口。
2. 后端保存真实文件到当前启用的存储平台。
3. `file_detail` 记录真实存储信息，包含 platform、path、filename、url 等字段。
4. 上传接口返回 `/boylu/file/content/{fileId}`，文章封面响应式变体也使用该网关地址。
5. 浏览器访问网关地址时，后端按 `fileId` 查真实目标，并把真实目标写入 Redis 短期缓存。
6. 本地文件目标会跳转或读取到 `/localFile/...`，由 Nginx 对静态文件做长期缓存。

## 本地存储配置

生产环境建议 `sys_file_oss.domain` 使用相对路径：

```text
/localFile/
```

不要写成：

```text
https://your-domain.example/localFile/
```

原因是服务器 IP、域名、HTTPS 协议以后都可能变化。相对路径会自动跟随当前站点域名。

## 兼容规则

这些旧地址必须继续可读，不允许删除兼容入口：

```text
/localFile/...
/file/content/{id}
/file/view/{id}
/boylu/file/content/{id}
/boylu/file/view/{id}
/mojian/file/content/{id}
/mojian/file/view/{id}
```

`/localFile/...` 是真实静态文件地址和历史数据兼容地址，不再作为新业务字段的首选保存格式。

## 样式图缓存

本地图片样式图使用：

```text
/img/local/<base64url旧地址>!w320.webp
```

首次请求由后端生成缓存图，并写入：

```text
/localFile/img-cache/...
```

缓存文件按原图内容 hash、宽度和格式命名。只允许新增或清理 `img-cache`，不删除、不移动、不覆盖任何旧原图。

## 迁移服务器清单

1. 迁移数据库，重点保留 `file_detail` 和所有业务表里的图片字段。
2. 迁移文件目录：`/opt/boylu-blog/storage/`。
3. 新服务器 Nginx 保持 `/localFile/` 指向新的存储目录。
4. 确认 `sys_file_oss.domain` 是 `/localFile/`。
5. 确认后端环境变量 `FILE_PUBLIC_PREFIX` 默认或显式为 `/boylu/file/content/`。

## 排查图片丢失

1. 如果 `/boylu/file/content/{id}` 返回 404，先查 `file_detail` 是否存在该 `id`。
2. 如果返回 302 但图片 404，检查 `file_detail.url` 或 `path + filename` 对应的真实文件是否存在。
3. 如果只有旧图片失败，优先把旧的 `http://旧IP/localFile/...` 改成 `/localFile/...`，不要改成新 IP。
4. 如果后台预览失败，检查前端是否使用 `normalizeImageUrl` 归一化图片地址。

## 性能约定

1. 图片真实静态资源由 Nginx `/localFile/` 直接提供，缓存 365 天并使用 `immutable`。
2. 文件网关 `/boylu/file/content/{id}` 只做轻量跳转或本地读取，并用 Redis 缓存真实地址。
3. 博客列表和正文图片优先使用 `/img/local/...` 小 WebP 样式图，失败后回退原图和默认图。
4. 后台上传允许 `jpg/png/gif/webp`，上传前后端都保留大小和像素尺寸保护。
