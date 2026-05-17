package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boylu.common.Constants;
import com.boylu.common.ResultCode;
import com.boylu.dto.article.ArticleQueryDto;
import com.boylu.dto.article.NotionImportDto;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysCategory;
import com.boylu.entity.SysTag;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.mapper.SysCategoryMapper;
import com.boylu.mapper.SysTagMapper;
import com.boylu.service.SysArticleService;
import com.boylu.service.notion.NotionImportService;
import com.boylu.service.seo.BaiduPushService;
import com.boylu.utils.AiUtil;
import com.boylu.utils.CoverImageUtil;
import com.boylu.utils.HtmlSanitizerUtil;
import com.boylu.utils.LocalFileUrlNormalizeUtil;
import com.boylu.utils.PageUtil;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.NotionImportResultVo;
import com.boylu.vo.article.SysArticleDetailVo;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysArticleServiceImpl extends ServiceImpl<SysArticleMapper, SysArticle> implements SysArticleService {

    private final SysTagMapper sysTagMapper;

    private final AiUtil aiUtil;
    private final SysCategoryMapper sysCategoryMapper;
    private final RedisUtil redisUtil;

    private final BaiduPushService baiduPushService;

    private final NotionImportService notionImportService;

    @Override
    public IPage<ArticleListVo> selectPage(ArticleQueryDto articleQueryDto) {
        return baseMapper.selectPageList(PageUtil.getPage(), articleQueryDto);
    }

    @Override
    public SysArticleDetailVo detail(Integer id) {
        SysArticle sysArticle = baseMapper.selectById(id);

        SysArticleDetailVo sysArticleDetailVo = new SysArticleDetailVo();
        BeanUtils.copyProperties(sysArticle, sysArticleDetailVo);
        sysArticleDetailVo.setCoverImage(CoverImageUtil.fromJson(sysArticle.getCoverImage(), sysArticle.getCover(), sysArticle.getTitle()));
        normalizeArticleDetail(sysArticleDetailVo);

        SysCategory sysCategory = sysCategoryMapper.selectById(sysArticle.getCategoryId());
        sysArticleDetailVo.setCategoryName(sysCategory.getName());

        //获取标签
        List<String> tags = sysTagMapper.getTagNameByArticleId(id);
        sysArticleDetailVo.setTags(tags);
        return sysArticleDetailVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean add(SysArticleDetailVo sysArticle) {

        SysArticle obj = new SysArticle();
        BeanUtils.copyProperties(sysArticle, obj);
        obj.setCoverImage(resolveCoverImageJson(sysArticle));
        normalizeArticleFileUrls(obj);
        obj.setUserId(StpUtil.getLoginIdAsLong());

        //添加分类
        addCategory(sysArticle, obj);
        baseMapper.insert(obj);
        sysArticle.setId(obj.getId());
        clearHomePostsCache();
        clearArticleDetailCache(obj.getId());

        addTags(sysArticle, obj);
        triggerBaiduPushIfPublished(obj);

        ThreadUtil.execAsync(() -> {
            String res = aiUtil.send(obj.getContent() + "请提供一段简短的介绍描述该文章的内容");
            if (StringUtils.isNotBlank(res)) {
                obj.setAiDescribe(res);
                baseMapper.updateById(obj);
            }
        });
        return true;
    }




    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SysArticleDetailVo sysArticle) {

        SysArticle obj = new SysArticle();
        BeanUtils.copyProperties(sysArticle, obj);
        obj.setCoverImage(resolveCoverImageJson(sysArticle));
        normalizeArticleFileUrls(obj);

        //没有管理员权限就只能修改自己的文章
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            SysArticle article = baseMapper.selectById(sysArticle.getId());
            if (article.getUserId() != StpUtil.getLoginIdAsLong()) {
                throw new ServiceException("只能修改自己的文章");
            }
        }

        addCategory(sysArticle, obj);
        baseMapper.updateById(obj);
        clearHomePostsCache();
        clearArticleDetailCache(obj.getId());

        //先删除标签在新增标签
        sysTagMapper.deleteArticleTagsByArticleIds(Collections.singletonList(obj.getId()));
        addTags(sysArticle, obj);
        triggerBaiduPushIfPublished(obj);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotionImportResultVo importFromNotion(NotionImportDto dto) {
        if (dto != null) {
            dto.setImportImages(false);
        }
        NotionImportService.ImportPageResult importResult = notionImportService.importPage(dto);
        SysArticleDetailVo article = importResult.getArticle();
        SysArticle existingArticle = findExistingNotionArticle(article.getOriginalUrl());
        ensureNotionImportHasContent(importResult, existingArticle != null);
        boolean updated = existingArticle != null;
        saveImportedArticle(article, existingArticle);
        queueNotionImageLocalization(article.getId());
        return NotionImportResultVo.builder()
                .articleId(article.getId())
                .title(article.getTitle())
                .importedBlocks(importResult.getImportedBlocks())
                .updated(updated)
                .imageLocalizationQueued(true)
                .warnings(importResult.getWarnings())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotionImportResultVo syncNotionArticle(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new ServiceException("文章ID不合法");
        }
        SysArticle existingArticle = baseMapper.selectById(articleId);
        if (existingArticle == null) {
            throw new ServiceException("文章不存在");
        }
        if (StringUtils.isBlank(existingArticle.getOriginalUrl())
                || !StringUtils.containsIgnoreCase(existingArticle.getOriginalUrl(), "notion.so")) {
            throw new ServiceException("当前文章不是 Notion 导入文章，无法同步");
        }

        SysArticleDetailVo currentDetail = detail(articleId.intValue());
        NotionImportDto dto = new NotionImportDto();
        dto.setPageUrl(existingArticle.getOriginalUrl());
        dto.setCategoryName(currentDetail.getCategoryName());
        dto.setTags(currentDetail.getTags());
        dto.setReadType(existingArticle.getReadType());
        dto.setStatus(existingArticle.getStatus());
        dto.setIsOriginal(existingArticle.getIsOriginal());
        dto.setIsStick(existingArticle.getIsStick());
        dto.setIsCarousel(existingArticle.getIsCarousel());
        dto.setIsRecommend(existingArticle.getIsRecommend());
        dto.setImportImages(false);

        NotionImportService.ImportPageResult importResult = notionImportService.importPage(dto);
        SysArticleDetailVo article = importResult.getArticle();
        ensureNotionImportHasContent(importResult, true);
        article.setId(existingArticle.getId());
        saveImportedArticle(article, existingArticle);
        queueNotionImageLocalization(article.getId());
        return NotionImportResultVo.builder()
                .articleId(article.getId())
                .title(article.getTitle())
                .importedBlocks(importResult.getImportedBlocks())
                .updated(true)
                .imageLocalizationQueued(true)
                .warnings(importResult.getWarnings())
                .build();
    }

    private void ensureNotionImportHasContent(NotionImportService.ImportPageResult importResult, boolean updatingExistingArticle) {
        SysArticleDetailVo article = importResult == null ? null : importResult.getArticle();
        boolean emptyContent = article == null
                || importResult.getImportedBlocks() == null
                || importResult.getImportedBlocks() <= 0
                || StringUtils.isBlank(article.getContentMd());
        if (!emptyContent) {
            return;
        }
        if (updatingExistingArticle) {
            throw new ServiceException("Notion 本次没有读取到正文内容，已保护旧文章，不会覆盖原内容");
        }
        throw new ServiceException("Notion 页面没有可导入的正文内容，请确认页面已共享给 Integration，且页面正文不为空");
    }

    private void saveImportedArticle(SysArticleDetailVo article, SysArticle existingArticle) {
        SysArticle obj = new SysArticle();
        BeanUtils.copyProperties(article, obj);
        obj.setCoverImage(resolveCoverImageJson(article));
        normalizeArticleFileUrls(obj);
        obj.setUserId(resolveImportedArticleUserId(existingArticle));

        addCategory(article, obj);
        if (existingArticle == null) {
            baseMapper.insert(obj);
            article.setId(obj.getId());
        } else {
            obj.setId(existingArticle.getId());
            baseMapper.updateById(obj);
            article.setId(existingArticle.getId());
            sysTagMapper.deleteArticleTagsByArticleIds(Collections.singletonList(obj.getId()));
        }

        clearHomePostsCache();
        clearArticleDetailCache(obj.getId());
        addTags(article, obj);
        if (existingArticle == null || (existingArticle.getStatus() != Constants.YES && obj.getStatus() == Constants.YES)) {
            triggerBaiduPushIfPublished(obj);
        }
    }

    private Long resolveImportedArticleUserId(SysArticle existingArticle) {
        if (existingArticle != null && existingArticle.getUserId() != null) {
            return existingArticle.getUserId();
        }
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
            // Scheduled Notion sync has no login context; fall back to the first admin user id.
        }
        return 1L;
    }

    private SysArticle findExistingNotionArticle(String sourceUrl) {
        if (StringUtils.isBlank(sourceUrl)) {
            return null;
        }
        String pageId = notionImportService.extractPageIdSafely(sourceUrl);
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getUserId, SysArticle::getOriginalUrl, SysArticle::getStatus)
                .eq(SysArticle::getOriginalUrl, sourceUrl);
        if (StringUtils.isNotBlank(pageId)) {
            String compactPageId = pageId.replace("-", "");
            wrapper.or(item -> item.like(SysArticle::getOriginalUrl, pageId)
                    .or()
                    .like(SysArticle::getOriginalUrl, compactPageId));
        }
        wrapper.last("limit 1");
        return baseMapper.selectOne(wrapper);
    }

    private void queueNotionImageLocalization(Long articleId) {
        Runnable task = () -> ThreadUtil.execAsync(() -> localizeNotionArticleImages(articleId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void localizeNotionArticleImages(Long articleId) {
        try {
            SysArticle article = baseMapper.selectById(articleId);
            NotionImportService.LocalizeImagesResult result = notionImportService.localizeArticleImages(article);
            if (result.getArticle() == null) {
                return;
            }
            baseMapper.updateById(result.getArticle());
            clearHomePostsCache();
            clearArticleDetailCache(articleId);
            log.info("Notion article images localized, articleId={}, changedFields={}, warnings={}",
                    articleId, result.getChangedCount(), result.getWarnings());
        } catch (Exception ex) {
            log.warn("Failed to localize Notion article images, articleId={}", articleId, ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(List<Long> ids) {

        //没有管理员权限就只能删除自己的文章
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            List<SysArticle> sysArticles = baseMapper.selectBatchIds(ids);
            for (SysArticle sysArticle : sysArticles) {
                if (sysArticle.getUserId() != StpUtil.getLoginIdAsLong()) {
                    throw new RuntimeException("只能删除自己的文章");
                }
            }
        }

        baseMapper.deleteBatchIds(ids);
        sysTagMapper.deleteArticleTagsByArticleIds(ids);
        clearHomePostsCache();
        clearArticleDetailCache(ids);
        return true;
    }

    @Override
    public boolean updateById(SysArticle entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            clearHomePostsCache();
            clearArticleDetailCache(entity.getId());
            triggerBaiduPushIfPublished(entity);
        }
        return updated;
    }

    @Override
    public Boolean pushToBaidu(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new ServiceException("文章ID不合法");
        }
        SysArticle article = baseMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        if (article.getStatus() != Constants.YES) {
            throw new ServiceException("仅已发布文章支持推送");
        }
        ThreadUtil.execAsync(() -> {
            baiduPushService.pushArticleUrl(articleId);
            SysArticle updateEntity = new SysArticle();
            updateEntity.setId(articleId);
            updateEntity.setIsBaiduPushed(1);
            baseMapper.updateById(updateEntity);
        });
        return true;
    }

    @Override
    public Integer pushRecentPublishedToBaidu(Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 200 : limit, 500));
        List<SysArticle> articles = baseMapper.selectList(new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getStatus, SysArticle::getCreateTime)
                .eq(SysArticle::getStatus, Constants.YES)
                .eq(SysArticle::getIsBaiduPushed, 0)
                .orderByDesc(SysArticle::getCreateTime)
                .last("limit " + safeLimit));
        if (articles == null || articles.isEmpty()) {
            return 0;
        }
        List<String> urls = new ArrayList<String>();
        List<Long> ids = new ArrayList<>();
        for (SysArticle article : articles) {
            if (article == null || article.getId() == null) {
                continue;
            }
            urls.add("https://boylu.cn/article/" + article.getId());
            ids.add(article.getId());
        }
        ThreadUtil.execAsync(() -> {
            baiduPushService.pushUrls(urls, "manual-recent-" + safeLimit);
            SysArticle updateEntity = new SysArticle();
            updateEntity.setIsBaiduPushed(1);
            baseMapper.update(updateEntity, new LambdaQueryWrapper<SysArticle>().in(SysArticle::getId, ids));
        });
        return urls.size();
    }


    @Override
    public void reptile(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements title  = document.getElementsByClass("title-article");
            Elements tags  = document.getElementsByClass("tag-link");
            Elements content  = document.getElementsByClass("article_content");
            if (StringUtils.isBlank(content.toString())) {
                throw new ServiceException(ResultCode.CRAWLING_ARTICLE_FAILED.getDesc());
            }

            //爬取的是HTML内容，需要转成MD格式的内容
            String newContent = content.get(0).toString().replaceAll("<code>", "<code class=\"lang-java\">");
            String markdown = FlexmarkHtmlConverter.builder(new MutableDataSet()).build().convert(newContent)
                    .replace("lang-java","java");

            SysArticle entity = SysArticle.builder().userId(StpUtil.getLoginIdAsLong()).contentMd(markdown)
                    .isOriginal(Constants.NO).originalUrl(url)
                    .title(title.get(0).text()).cover("https://api.btstu.cn/sjbz/api.php?lx=dongman&format=images").content(newContent).build();
            normalizeArticleFileUrls(entity);

            baseMapper.insert(entity);
            clearHomePostsCache();
            //为该文章添加标签
            List<Integer> tagIds = new ArrayList<>();
            tags.forEach(item ->{
                String tag = item.text();
                SysTag result = sysTagMapper.selectOne(new LambdaQueryWrapper<SysTag>().eq(SysTag::getName,tag ));
                if (result == null){
                    result = SysTag.builder().name(tag).build();
                    sysTagMapper.insert(result);
                }
                tagIds.add(result.getId());
            });
            sysTagMapper.addArticleTagRelations(entity.getId(),tagIds);

            log.info("文章抓取成功，articleId={}", entity.getId());
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private void addCategory(SysArticleDetailVo sysArticle, SysArticle obj) {
        SysCategory sysCategory = sysCategoryMapper.selectOne(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getName, sysArticle.getCategoryName()));
        if (sysCategory == null) {
            sysCategory = SysCategory.builder().name(sysArticle.getCategoryName()).build();
            sysCategoryMapper.insert(sysCategory);
        }
        obj.setCategoryId(sysCategory.getId());
    }

    private void addTags(SysArticleDetailVo sysArticle, SysArticle obj) {
        //添加标签
        if (sysArticle.getTags() == null || sysArticle.getTags().isEmpty()) {
            return;
        }
        List<Integer> tagIds = new ArrayList<>();
        for (String tag : sysArticle.getTags()) {
            SysTag sysTag = sysTagMapper.selectOne(new LambdaQueryWrapper<SysTag>().eq(SysTag::getName, tag));
            if (sysTag == null) {
                sysTag = SysTag.builder().name(tag).build();
                sysTagMapper.insert(sysTag);
            }
            tagIds.add(sysTag.getId());
        }
        sysTagMapper.addArticleTagRelations(obj.getId(), tagIds);
    }

    private void normalizeArticleFileUrls(SysArticle article) {
        if (article == null) {
            return;
        }
        article.setCover(LocalFileUrlNormalizeUtil.normalizeUrl(article.getCover()));
        article.setContent(HtmlSanitizerUtil.sanitizeUserRichText(LocalFileUrlNormalizeUtil.normalizeText(article.getContent())));
        article.setContentMd(LocalFileUrlNormalizeUtil.normalizeText(article.getContentMd()));
    }

    private void normalizeArticleDetail(SysArticleDetailVo article) {
        if (article == null) {
            return;
        }
        article.setCover(LocalFileUrlNormalizeUtil.normalizeUrl(article.getCover()));
        article.setContent(LocalFileUrlNormalizeUtil.normalizeText(article.getContent()));
        article.setContentMd(LocalFileUrlNormalizeUtil.normalizeText(article.getContentMd()));
    }

    private void clearHomePostsCache() {
        redisUtil.delete(com.boylu.common.RedisConstants.HOME_POSTS_CACHE_KEY);
    }

    private void clearArticleDetailCache(Long articleId) {
        if (articleId == null) {
            return;
        }
        redisUtil.delete(com.boylu.common.RedisConstants.ARTICLE_DETAIL_CACHE_KEY + articleId);
    }

    private void clearArticleDetailCache(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return;
        }
        for (Long articleId : articleIds) {
            clearArticleDetailCache(articleId);
        }
    }

    private void triggerBaiduPushIfPublished(SysArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        if (article.getStatus() != Constants.YES) {
            return;
        }
        ThreadUtil.execAsync(() -> {
            baiduPushService.pushArticleUrl(article.getId());
            SysArticle updateEntity = new SysArticle();
            updateEntity.setId(article.getId());
            updateEntity.setIsBaiduPushed(1);
            baseMapper.updateById(updateEntity);
        });
    }

    private String resolveCoverImageJson(SysArticleDetailVo article) {
        if (article == null) {
            return null;
        }
        if (article.getCoverImage() != null) {
            return CoverImageUtil.toJson(article.getCoverImage());
        }
        return CoverImageUtil.toJson(CoverImageUtil.fromJson(null, article.getCover(), article.getTitle()));
    }
}
