# 项目维护与清理规范

本文档用于避免仓库再次混入依赖目录、构建产物、部署压缩包和临时恢复文件。

## 提交前检查

每次提交前先执行：

```powershell
git status --short
```

重点确认不要出现：

- `node_modules/`
- `dist/`
- `target/`
- `*.tar.gz`
- `*.zip`
- `*_deploy*`
- `_restore_*`
- `e2e/playwright-report/`
- `e2e/test-results/`

## 常见目录处理

| 类型 | 处理方式 |
| --- | --- |
| 前端依赖 | 保留本地，禁止提交 |
| 前端构建产物 | 可删除，需要时重新 `npm run build` |
| 后端 Maven target | 可删除，需要时重新 `mvn package` |
| Playwright 报告 | 可删除，需要时重新运行测试 |
| 临时部署包 | 放到 `release/` 或移出仓库 |
| 正式部署配置 | 放到 `deploy/` |
| 项目说明文档 | 放到 `docs/` |
| 本地辅助脚本 | 放到 `scripts/` |

## 重新安装依赖

```powershell
cd E:\做题\shiyi-blog-master\blog-web
npm install
```

```powershell
cd E:\做题\shiyi-blog-master\blog-admin
npm install
```

```powershell
cd E:\做题\shiyi-blog-master\e2e
npm install
```

## 重新构建

```powershell
cd E:\做题\shiyi-blog-master\blog-web
npm run build
```

```powershell
cd E:\做题\shiyi-blog-master\blog-admin
npm run build
```

```powershell
cd E:\做题\shiyi-blog-master\blog
mvn clean package
```

## 回归测试

先启动前台和后台，再运行：

```powershell
powershell -ExecutionPolicy Bypass -File E:\做题\shiyi-blog-master\scripts\run-playwright-regression.ps1
```

如果缺少浏览器：

```powershell
cd E:\做题\shiyi-blog-master\e2e
npm run install:browsers
```

## 当前已归档内容

临时部署包、恢复文件和 `.deploy-temp` 已统一移动到：

```text
release/manual-archive-20260422/
```

这些文件用于短期回滚参考，不建议长期保留在仓库里。

