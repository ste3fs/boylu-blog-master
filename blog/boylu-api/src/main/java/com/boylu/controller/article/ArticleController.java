package com.boylu.controller.article;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.annotation.AccessLimit;
import com.boylu.entity.SysCategory;
import com.boylu.service.ArticleService;
import com.boylu.vo.article.ArchiveListVo;
import com.boylu.vo.article.ArticleDetailVo;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.CategoryListVo;
import com.boylu.vo.article.HomeArticleVo;
import com.boylu.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
@Api(tags = "门户-文章管理")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/list")
    @ApiOperation(value = "获取文章列表")
    public Result<IPage<ArticleListVo>> getArticleList(Integer tagId, Integer categoryId,String keyword) {
        return Result.success(articleService.getArticleList(tagId,categoryId,keyword));
    }

    @GetMapping("/home-list")
    @ApiOperation(value = "获取首页文章轻量列表")
    public Result<IPage<HomeArticleVo>> getHomeArticleList(Integer tagId, Integer categoryId, String keyword,
                                                           HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=60, s-maxage=300, stale-while-revalidate=86400");
        return Result.success(articleService.getHomeArticleList(tagId, categoryId, keyword));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation(value = "获取文章详情")
    public Result<ArticleDetailVo> getArticleDetail(@PathVariable Long id, HttpServletRequest request,
                                                    HttpServletResponse response) {
        ArticleDetailVo detail = articleService.getArticleDetail(id);
        if (detail == null) {
            response.setHeader("Cache-Control", "public, max-age=30, stale-while-revalidate=60");
            return Result.success(null);
        }
        String etag = buildArticleDetailEtag(detail);
        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "public, max-age=30, stale-while-revalidate=60");
        if (isSameEtag(request.getHeader("If-None-Match"), etag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
        }
        return Result.success(detail);
    }

    private String buildArticleDetailEtag(ArticleDetailVo detail) {
        String content = StringUtils.hasText(detail.getContentMd()) ? detail.getContentMd() : detail.getContent();
        String signature = String.valueOf(detail.getId()) + "|"
                + String.valueOf(detail.getQuantity()) + "|"
                + String.valueOf(detail.getCommentNum()) + "|"
                + String.valueOf(detail.getLikeNum()) + "|"
                + String.valueOf(detail.getCreateTime()) + "|"
                + String.valueOf(detail.getTitle()) + "|"
                + String.valueOf(detail.getCover()) + "|"
                + String.valueOf(detail.getAiDescribe()) + "|"
                + DigestUtils.md5DigestAsHex(String.valueOf(content).getBytes(StandardCharsets.UTF_8));
        return "\"" + DigestUtils.md5DigestAsHex(signature.getBytes(StandardCharsets.UTF_8)) + "\"";
    }

    private boolean isSameEtag(String ifNoneMatch, String etag) {
        if (!StringUtils.hasText(ifNoneMatch) || !StringUtils.hasText(etag)) {
            return false;
        }
        String normalized = ifNoneMatch.trim();
        if (normalized.startsWith("W/")) {
            normalized = normalized.substring(2).trim();
        }
        return etag.equals(normalized);
    }

    @GetMapping("/archive")
    @ApiOperation(value = "获取归档")
    public Result<List<ArchiveListVo>> getArticleArchive() {
        return Result.success(articleService.getArticleArchive());
    }

    @GetMapping("/categories")
    @ApiOperation(value = "获取分类")
    public Result<List<CategoryListVo>> getArticleCategories() {
        return Result.success(articleService.getArticleCategories());
    }

    @GetMapping("/categorie-all")
    @ApiOperation(value = "获取所有分类")
    public Result<List<SysCategory>> getCategoryAll() {
        return Result.success(articleService.getCategoryAll());
    }


    @GetMapping("/getCarousels")
    @ApiOperation(value = "获取轮播文章")
    public Result<List<ArticleListVo>> getCarouselArticle() {
        return Result.success(articleService.getCarouselArticle());
    }

    @GetMapping("/getRecommends")
    @ApiOperation(value = "获取推荐文章")
    public Result<List<ArticleListVo>> getRecommendArticle() {
        return Result.success(articleService.getRecommendArticle());
    }

    @SaCheckLogin
    @GetMapping("/like/{id}")
    @AccessLimit(time = 5, count = 1)
    @ApiOperation(value = "点赞文章")
    public Result<Boolean> like(@PathVariable Long id) {
        return Result.success(articleService.like(id));
    }
}
