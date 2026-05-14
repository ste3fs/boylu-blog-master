package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.boylu.common.RedisConstants;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysCategory;
import com.boylu.entity.SysNotifications;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.mapper.SysCategoryMapper;
import com.boylu.service.ArticleService;
import com.boylu.utils.IpUtil;
import com.boylu.utils.CoverImageUtil;
import com.boylu.utils.NotificationsUtil;
import com.boylu.common.PageQuery;
import com.boylu.utils.PageUtil;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.article.ArchiveListVo;
import com.boylu.vo.article.ArticleDetailVo;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.CategoryListVo;
import com.boylu.vo.article.HomeArticleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final String ANONYMOUS_VIEW_COOKIE = "boylu_viewer_id";

    private static final Duration ANONYMOUS_VIEW_COOKIE_AGE = Duration.ofDays(365);

    private final SysArticleMapper sysArticleMapper;

    private final SysCategoryMapper sysCategoryMapper;

    private final RedisUtil redisUtil;

    private final NotificationsUtil notificationsUtil;

    @Override
    public IPage<ArticleListVo> getArticleList(Integer tagId, Integer categoryId, String keyword) {
        IPage<ArticleListVo> page = sysArticleMapper.getArticleListApi(PageUtil.getPage(), tagId, categoryId, keyword);
        applyRealtimeQuantity(page.getRecords());
        hydrateArticleListCovers(page.getRecords());
        return page;
    }

    @Override
    public IPage<HomeArticleVo> getHomeArticleList(Integer tagId, Integer categoryId, String keyword) {
        PageQuery pageQuery = PageUtil.getPageQuery();
        boolean cacheable = isHomePostsCacheable(pageQuery, tagId, categoryId, keyword);

        if (cacheable) {
            Page<HomeArticleVo> cachedPage = getCachedHomePosts();
            if (cachedPage != null) {
                try {
                    hydrateHomeArticles(cachedPage.getRecords());
                    return cachedPage;
                } catch (Exception ex) {
                    redisUtil.delete(RedisConstants.HOME_POSTS_CACHE_KEY);
                }
            }
        }

        Page<Object> page = new Page<Object>(pageQuery.getPageNum(), pageQuery.getPageSize());
        IPage<HomeArticleVo> result = sysArticleMapper.getHomeArticleListApi(page, tagId, categoryId, keyword);
        hydrateHomeArticles(result.getRecords());

        if (cacheable) {
            redisUtil.set(RedisConstants.HOME_POSTS_CACHE_KEY, JSON.toJSONString(result), 300, TimeUnit.SECONDS);
        }

        return result;
    }

    @Override
    public ArticleDetailVo getArticleDetail(Long id) {
        ArticleDetailVo detailVo = getCachedArticleDetail(id);
        if (detailVo == null) {
            return null;
        }

        Object userId = StpUtil.getLoginIdDefaultNull();
        if (userId != null) {
            detailVo.setIsLike(sysArticleMapper.getUserIsLike(id, Integer.parseInt(userId.toString())));
        }

        detailVo.setQuantity(recordRealtimeQuantity(id, detailVo.getQuantity(), userId));
        return detailVo;
    }

    private ArticleDetailVo getCachedArticleDetail(Long id) {
        if (id == null) {
            return null;
        }
        String cacheKey = RedisConstants.ARTICLE_DETAIL_CACHE_KEY + id;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                ArticleDetailVo cachedVo = parseCachedArticleDetail(cached);
                if (cachedVo != null) {
                    return cachedVo;
                }
            }
        } catch (Exception ex) {
            redisUtil.delete(cacheKey);
        }

        ArticleDetailVo loaded = sysArticleMapper.getArticleDetail(id);
        if (loaded == null) {
            return null;
        }
        try {
            redisUtil.set(cacheKey, JSON.toJSONString(loaded), 120, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            redisUtil.delete(cacheKey);
        }
        return loaded;
    }

    private ArticleDetailVo parseCachedArticleDetail(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof ArticleDetailVo) {
            return JSON.parseObject(JSON.toJSONString(cached), ArticleDetailVo.class);
        }
        return JSON.parseObject(String.valueOf(cached), ArticleDetailVo.class);
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

    private boolean isHomePostsCacheable(PageQuery pageQuery, Integer tagId, Integer categoryId, String keyword) {
        return pageQuery != null
                && pageQuery.getPageNum() != null
                && pageQuery.getPageSize() != null
                && pageQuery.getPageNum() == 1
                && pageQuery.getPageSize() == 10
                && tagId == null
                && categoryId == null
                && !StringUtils.hasText(keyword);
    }

    private Page<HomeArticleVo> getCachedHomePosts() {
        try {
            Object cached = redisUtil.get(RedisConstants.HOME_POSTS_CACHE_KEY);
            if (cached == null) {
                return null;
            }
            return parseCachedHomePosts(cached);
        } catch (Exception ex) {
            redisUtil.delete(RedisConstants.HOME_POSTS_CACHE_KEY);
            return null;
        }
    }

    private Page<HomeArticleVo> parseCachedHomePosts(Object cached) {
        JSONObject object = cached instanceof JSONObject
                ? (JSONObject) cached
                : JSON.parseObject(String.valueOf(cached));
        if (object == null || object.isEmpty()) {
            return null;
        }

        long current = object.getLongValue("current");
        long size = object.getLongValue("size");
        Page<HomeArticleVo> page = new Page<HomeArticleVo>(current <= 0 ? 1 : current, size <= 0 ? 10 : size);
        page.setTotal(object.getLongValue("total"));
        page.setPages(object.getLongValue("pages"));

        JSONArray records = object.getJSONArray("records");
        List<HomeArticleVo> articles = new ArrayList<HomeArticleVo>();
        if (records != null) {
            for (Object record : records) {
                if (record == null) {
                    continue;
                }
                articles.add(record instanceof HomeArticleVo
                        ? (HomeArticleVo) record
                        : JSON.parseObject(JSON.toJSONString(record), HomeArticleVo.class));
            }
        }
        page.setRecords(articles);
        return page;
    }

    private void hydrateHomeArticles(List<HomeArticleVo> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        Map<Object, Object> quantityMap = redisUtil.hGetAll(RedisConstants.ARTICLE_QUANTITY);
        for (HomeArticleVo article : articles) {
            if (article == null) {
                continue;
            }
            if (!StringUtils.hasText(article.getSlug()) && article.getId() != null) {
                article.setSlug(String.valueOf(article.getId()));
            }
            if (article.getCoverImage() == null
                    || StringUtils.hasText(article.getCoverImageJson())
                    || StringUtils.hasText(article.getLegacyCover())) {
                article.setCoverImage(CoverImageUtil.fromJson(article.getCoverImageJson(), article.getLegacyCover(), article.getTitle()));
            }
            if (article.getReadingTime() == null || article.getReadingTime() <= 0) {
                article.setReadingTime(estimateReadingTime(article.getExcerpt()));
            }
            if (article.getId() != null) {
                Object redisValue = quantityMap.get(article.getId().toString());
                article.setViews(resolveQuantity(article.getViews(), redisValue));
            } else if (article.getViews() == null) {
                article.setViews(0);
            }
        }
    }

    private void hydrateArticleListCovers(List<ArticleListVo> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        for (ArticleListVo article : articles) {
            if (article == null) {
                continue;
            }
            article.setCoverImage(CoverImageUtil.fromJson(article.getCoverImageJson(), article.getCover(), article.getTitle()));
        }
    }

    private int estimateReadingTime(String text) {
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(source.length() / 180.0));
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

    private Integer recordRealtimeQuantity(Long articleId, Integer dbQuantity, Object userId) {
        if (articleId == null) {
            return dbQuantity == null ? 0 : dbQuantity;
        }
        String hashKey = articleId.toString();
        Object cachedValue = redisUtil.hGet(RedisConstants.ARTICLE_QUANTITY, hashKey);
        int currentQuantity = resolveQuantity(dbQuantity, cachedValue);
        int nextQuantity = currentQuantity + 1;
        redisUtil.hSet(RedisConstants.ARTICLE_QUANTITY, hashKey, nextQuantity);
        return nextQuantity;
    }

    private List<String> resolveViewerKeys(Object userId) {
        if (userId != null && !String.valueOf(userId).trim().isEmpty()) {
            return Collections.singletonList("user:" + String.valueOf(userId).trim());
        }

        HttpServletRequest request = IpUtil.getRequest();
        Set<String> viewerKeys = new LinkedHashSet<String>();

        String cookieVisitorId = getCookieValue(request, ANONYMOUS_VIEW_COOKIE);
        if (!StringUtils.hasText(cookieVisitorId)) {
            cookieVisitorId = UUID.randomUUID().toString().replace("-", "");
            writeAnonymousViewerCookie(cookieVisitorId, request);
        }
        if (StringUtils.hasText(cookieVisitorId)) {
            viewerKeys.add("guest:cookie:" + cookieVisitorId);
        }

        String fallbackKey = buildFallbackViewerKey(request);
        if (StringUtils.hasText(fallbackKey)) {
            viewerKeys.add("guest:fallback:" + fallbackKey);
        }
        return new ArrayList<String>(viewerKeys);
    }

    private boolean hasAnyViewerKey(String viewedKey, List<String> viewerKeys) {
        for (String viewerKey : viewerKeys) {
            if (StringUtils.hasText(viewerKey) && Boolean.TRUE.equals(redisUtil.sIsMember(viewedKey, viewerKey))) {
                return true;
            }
        }
        return false;
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null || !StringUtils.hasText(cookieName)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private void writeAnonymousViewerCookie(String visitorId, HttpServletRequest request) {
        if (!StringUtils.hasText(visitorId)) {
            return;
        }
        HttpServletResponse response = getCurrentResponse();
        if (response == null) {
            return;
        }

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(ANONYMOUS_VIEW_COOKIE, visitorId)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(ANONYMOUS_VIEW_COOKIE_AGE);

        if (isSecureRequest(request)) {
            builder.secure(true);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private HttpServletResponse getCurrentResponse() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getResponse();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }

    private String buildFallbackViewerKey(HttpServletRequest request) {
        String userAgent = request == null ? "" : String.valueOf(request.getHeader("User-Agent"));
        String source = String.valueOf(IpUtil.getIp()) + "|" + userAgent;
        if (!StringUtils.hasText(source.replace("|", "").trim())) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
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
                .select(SysArticle::getId, SysArticle::getTitle, SysArticle::getCover, SysArticle::getCoverImage, SysArticle::getCreateTime)
                .orderByDesc(SysArticle::getCreateTime)
                .eq(conditionField, 1);

        List<SysArticle> sysArticles = sysArticleMapper.selectList(wrapper);

        if (sysArticles == null || sysArticles.isEmpty()) {
            return Collections.emptyList();
        }

        return sysArticles.stream().map(item -> ArticleListVo.builder()
                .id(item.getId())
                .cover(item.getCover())
                .coverImage(CoverImageUtil.fromJson(item.getCoverImage(), item.getCover(), item.getTitle()))
                .title(item.getTitle())
                .createTime(item.getCreateTime())
                .build()).collect(Collectors.toList());
    }
}
