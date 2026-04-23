# 图片存储与加载约定

## 目标

图片不要直接绑定服务器 IP，也不要在业务表里长期保存真实磁盘地址。项目统一保存和展示稳定的文件网关地址：

```text
/boylu/file/content/{fileId}
```

这个地址只依赖数据库里的 `file_detail.id`。以后迁移服务器时，只需要迁移数据库、`/opt/boylu-blog/storage/` 文件目录和 Nginx 配置，不需要批量修改文章、相册、头像里的图片地址。

## 当前链路

1. 后台上传组件调用 `/file/upload?source=xxx`。
2. 后端保存真实文件到当前启用的存储平台。
3. `file_detail` 记录真实存储信息。
4. 上传接口返回 `/boylu/file/content/{fileId}`。
5. 浏览器访问该地址时，后端按 `fileId` 查真实文件地址，并跳转到 `/localFile/...` 或外部对象存储地址。
6. Nginx 对 `/localFile/` 做静态文件缓存。

## 本地存储配置

生产环境建议 `sys_file_oss.domain` 使用相对路径：

```text
/localFile/
```

不要写成：

```text
http://111.229.123.234/localFile/
```

原因是服务器 IP、域名、HTTPS 协议以后都可能变化。相对路径会自动跟随当前站点域名。

## 迁移服务器清单

1. 迁移数据库，重点保留 `file_detail` 和所有业务表里的图片字段。
2. 迁移文件目录：`/opt/boylu-blog/storage/`。
3. 新服务器 Nginx 保持 `/localFile/` 指向新的存储目录。
4. 确认 `sys_file_oss.domain` 是 `/localFile/`。
5. 确认后端环境变量 `FILE_PUBLIC_PREFIX` 默认或显式为 `/boylu/file/content/`。

## 排查图片丢失

1. 如果 `/boylu/file/content/{id}` 返回 404，先查 `file_detail` 是否存在该 `id`。
2. 如果返回 302 但图片 404，检查 `file_detail.url` 对应的真实文件是否存在。
3. 如果只有旧图片失败，优先把旧的 `http://旧IP/localFile/...` 改成 `/localFile/...`，不要改成新 IP。
4. 如果后台预览失败，检查前端是否使用 `normalizeImageUrl` 归一化图片地址。

## 性能约定

1. 图片真实静态资源由 Nginx `/localFile/` 直接提供，缓存 30 天。
2. 文件网关 `/boylu/file/content/{id}` 只做一次轻量跳转，并用 Redis 缓存真实地址。
3. 后台上传允许 `jpg/png/gif/webp`。博客图片优先使用 `webp`，体积更小，加载更快。
