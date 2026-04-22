package com.mojian.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.mojian.common.RedisConstants;
import com.mojian.entity.SysArticle;
import com.mojian.entity.SysCategory;
import com.mojian.entity.SysNotifications;
import com.mojian.mapper.SysArticleMapper;
import com.mojian.mapper.SysCategoryMapper;
import com.mojian.service.ArticleService;
import com.mojian.utils.NotificationsUtil;
import com.mojian.utils.PageUtil;
import com.mojian.utils.RedisUtil;
import com.mojian.vo.article.ArchiveListVo;
import com.mojian.vo.article.ArticleDetailVo;
import com.mojian.vo.article.ArticleListVo;
import com.mojian.vo.article.CategoryListVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final SysArticleMapper sysArticleMapper;

    private final SysCategoryMapper sysCategoryMapper;

    private final RedisUtil redisUtil;

    private final NotificationsUtil notificationsUtil;

    @Override
    public IPage<ArticleListVo> getArticleList(Integer tagId, Integer categoryId, String keyword) {
        IPage<ArticleListVo> page = sysArticleMapper.getArticleListApi(PageUtil.getPage(), tagId, categoryId, keyword);
        applyRealtimeQuantity(page.getRecords());
        return page;
    }

    @Override
    public ArticleDetailVo getArticleDetail(Long id) {
        ArticleDetailVo detailVo = sysArticleMapper.getArticleDetail(id);
        if (detailVo == null) {
            return null;
        }

        Object userId = StpUtil.getLoginIdDefaultNull();
        if (userId != null) {
            detailVo.setIsLike(sysArticleMapper.getUserIsLike(id, Integer.parseInt(userId.toString())));
        }

        detailVo.setQuantity(increaseRealtimeQuantity(id, detailVo.getQuantity()));
        return detailVo;
    }

    @Override
    public List<ArchiveListVo> getArticleArchive() {
        List<ArchiveListVo> list = new ArrayList<ArchiveListVo>();
        List<Integer> years = sysArticleMapper.getArticleArchive();
        for (Integer year : years) {
            List<ArticleListVo> articleListVos = sysArticleMapper.getArticleByYear(year);
            list.add(new ArchiveListVo(year, articleListVos));
        }
        return list;
    }

    @Override
    public List<CategoryListVo> getArticleCategories() {
        return sysCategoryMapper.getArticleCategories();
    }

    @Override
    public List<ArticleListVo> getCarouselArticle() {
        return getArticlesByCondition(SysArticle::getIsCarousel);
    }

    @Override
    public List<ArticleListVo> getRecommendArticle() {
        return getArticlesByCondition(SysArticle::getIsRecommend);
    }

    @Override
    public Boolean like(Long articleId) {
        int userId = StpUtil.getLoginIdAsInt();
        Boolean isLike = sysArticleMapper.getUserIsLike(articleId, userId);
        if (isLike) {
            sysArticleMapper.unLike(articleId, userId);
        } else {
            sysArticleMapper.like(articleId, userId);
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    SysNotifications notifications = SysNotifications.builder()
                            .title("文章点赞通知")
                            .articleId(articleId)
                            .isRead(0)
                            .type("like")
                            .fromUserId(StpUtil.getLoginIdAsLong())
                            .build();
                    notificationsUtil.publish(notifications);
                }
            });
        }
        return true;
    }

    @Override
    public List<SysCategory> getCategoryAll() {
        return sysCategoryMapper.selectList(new LambdaQueryWrapper<SysCategory>()
                .orderByAsc(SysCategory::getSort));
    }

    private void applyRealtimeQuantity(List<ArticleListVo> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        Map<Object, Object> quantityMap = redisUtil.hGetAll(RedisConstants.ARTICLE_QUANTITY);
        for (ArticleListVo article : articles) {
            if (article == null || article.getId() == null) {
                continue;
            }
            Object redisValue = quantityMap.get(article.getId().toString());
            article.setQuantity(resolveQuantity(article.getQuantity(), redisValue));
        }
    }

    private Integer increaseRealtimeQuantity(Long articleId, Integer dbQuantity) {
        if (articleId == null) {
            return dbQuantity == null ? 0 : dbQuantity;
        }
        String hashKey = articleId.toString();
        Object cachedValue = redisUtil.hGet(RedisConstants.ARTICLE_QUANTITY, hashKey);
        int nextQuantity = resolveQuantity(dbQuantity, cachedValue) + 1;
        redisUtil.hSet(RedisConstants.ARTICLE_QUANTITY, hashKey, nextQuantity);
        return nextQuantity;
    }

    private Integer resolveQuantity(Integer dbQuantity, Object redisValue) {
        int databaseValue = dbQuantity == null ? 0 : dbQuantity.intValue();
        int realtimeValue = parseQuantity(redisValue);
        return Math.max(databaseValue, realtimeValue);
    }

    private int parseQuantity(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return Math.max(0, ((Number) value).intValue());
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return 0;
        }
        if (text.matches("\\d+")) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            String content = text.substring(1, text.length() - 1).trim();
            if (content.isEmpty()) {
                return 0;
            }
            return content.split("\\s*,\\s*").length;
        }
        return 1;
    }

    private List<ArticleListVo> getArticlesByCondition(SFunction<SysArticle, Object> conditionField) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getTitle, SysArticle::getCover, SysArticle::getCreateTime)
                .orderByDesc(SysArticle::getCreateTime)
                .eq(conditionField, 1);

        List<SysArticle> sysArticles = sysArticleMapper.selectList(wrapper);

        if (sysArticles == null || sysArticles.isEmpty()) {
            return Collections.emptyList();
        }

        return sysArticles.stream().map(item -> ArticleListVo.builder()
                .id(item.getId())
                .cover(item.getCover())
                .title(item.getTitle())
                .createTime(item.getCreateTime())
                .build()).collect(Collectors.toList());
    }
}
