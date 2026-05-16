package com.boylu.service.notion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.entity.SysArticle;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.service.SysArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionArticleSyncJob {

    private final SysArticleMapper sysArticleMapper;
    private final SysArticleService sysArticleService;

    @Value("${notion.auto-sync-enabled:true}")
    private boolean autoSyncEnabled;

    @Value("${notion.auto-sync-limit:20}")
    private int autoSyncLimit;

    @Scheduled(cron = "${notion.auto-sync-cron:0 20 4 * * ?}")
    public void syncNotionArticles() {
        if (!autoSyncEnabled) {
            return;
        }
        int safeLimit = Math.max(1, Math.min(autoSyncLimit, 100));
        List<SysArticle> articles = sysArticleMapper.selectList(new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getOriginalUrl)
                .isNotNull(SysArticle::getOriginalUrl)
                .like(SysArticle::getOriginalUrl, "notion.so")
                .orderByDesc(SysArticle::getUpdateTime)
                .last("limit " + safeLimit));
        if (articles == null || articles.isEmpty()) {
            return;
        }
        for (SysArticle article : articles) {
            try {
                sysArticleService.syncNotionArticle(article.getId());
            } catch (Exception ex) {
                log.warn("Scheduled Notion article sync failed, articleId={}", article.getId(), ex);
            }
        }
    }
}
