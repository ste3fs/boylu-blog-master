package com.boylu.controller.article;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.common.Result;
import com.boylu.dto.article.ArticleQueryDto;
import com.boylu.dto.article.NotionImportDto;
import com.boylu.entity.NotionArticleSyncLog;
import com.boylu.entity.SysArticle;
import com.boylu.service.SysArticleService;
import com.boylu.service.notion.NotionSyncLogService;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.NotionImportResultVo;
import com.boylu.vo.article.SysArticleDetailVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "文章管理")
@RequestMapping("/sys/article")
@RequiredArgsConstructor
public class SysArticleController {

    private final SysArticleService sysArticleService;

    private final NotionSyncLogService notionSyncLogService;

    @GetMapping("/list")
    @ApiOperation(value  = "文章列表")
    @SaCheckPermission("sys:article:list")
    public Result<IPage<ArticleListVo>> list(ArticleQueryDto articleQueryDto) {
        return Result.success(sysArticleService.selectPage(articleQueryDto));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation(value = "文章详情")
    public Result<SysArticleDetailVo> detail(@PathVariable Integer id) {
        return Result.success(sysArticleService.detail(id));
    }

    @PostMapping("/add")
    @ApiOperation(value = "新增文章")
    @SaCheckPermission("sys:article:add")
    public Result<Boolean> add(@RequestBody SysArticleDetailVo sysArticle) {
        return Result.success(sysArticleService.add(sysArticle));
    }

    @PostMapping("/import/notion")
    @ApiOperation(value = "导入 Notion 笔记")
    @SaCheckPermission("sys:article:add")
    public Result<NotionImportResultVo> importNotion(@RequestBody NotionImportDto dto) {
        return Result.success(sysArticleService.importFromNotion(dto));
    }

    @PostMapping("/sync/notion/{id}")
    @ApiOperation(value = "同步 Notion 笔记")
    @SaCheckPermission("sys:article:update")
    public Result<NotionImportResultVo> syncNotion(@PathVariable Long id) {
        return Result.success(sysArticleService.syncNotionArticle(id));
    }

    @GetMapping("/notion/logs")
    @ApiOperation(value = "最近 Notion 同步日志")
    @SaCheckPermission("sys:article:list")
    public Result<List<NotionArticleSyncLog>> recentNotionLogs() {
        return Result.success(notionSyncLogService.listRecent());
    }

    @GetMapping("/notion/logs/{articleId}")
    @ApiOperation(value = "Notion 同步日志")
    @SaCheckPermission("sys:article:list")
    public Result<List<NotionArticleSyncLog>> notionLogs(@PathVariable Long articleId) {
        return Result.success(notionSyncLogService.listByArticleId(articleId));
    }

    @DeleteMapping("/notion/logs/{ids}")
    @ApiOperation(value = "删除 Notion 同步日志")
    @SaCheckPermission("sys:article:update")
    public Result<Boolean> deleteNotionLogs(@PathVariable List<Long> ids) {
        return Result.success(notionSyncLogService.deleteByIds(ids));
    }

    @PutMapping("/update")
    @ApiOperation(value = "修改文章")
    @SaCheckPermission("sys:article:update")
    public Result<Boolean> update(@RequestBody SysArticleDetailVo sysArticle) {
        return Result.success(sysArticleService.update(sysArticle));
    }

    @PutMapping("/updateStatus")
    @ApiOperation(value = "修改状态")
    @SaCheckPermission("sys:article:updateStatus")
    public Result<Boolean> updateStatus(@RequestBody SysArticle sysArticle) {
        return Result.success(sysArticleService.updateById(sysArticle));
    }

    @PostMapping("/pushBaidu/{id}")
    @ApiOperation(value = "手动推送文章到百度")
    @SaCheckPermission("sys:article:update")
    public Result<Boolean> pushBaidu(@PathVariable Long id) {
        return Result.success(sysArticleService.pushToBaidu(id));
    }

    @PostMapping("/pushBaiduRecent")
    @ApiOperation(value = "手动推送最近发布文章到百度")
    @SaCheckPermission("sys:article:update")
    public Result<Integer> pushBaiduRecent(@RequestParam(defaultValue = "200") Integer limit) {
        return Result.success(sysArticleService.pushRecentPublishedToBaidu(limit));
    }

    @DeleteMapping("/delete/{ids}")
    @ApiOperation(value = "删除文章")
    @SaCheckPermission("sys:article:delete")
    public Result<Boolean> delete(@PathVariable List<Long> ids) {
        return Result.success(sysArticleService.delete(ids));
    }

    @GetMapping("/reptile")
    @ApiOperation(value = "爬取文章")
    @SaCheckPermission("sys:article:reptile")
    public Result<Void> reptile(String url){
        sysArticleService.reptile(url);
        return Result.success();
    }
}
