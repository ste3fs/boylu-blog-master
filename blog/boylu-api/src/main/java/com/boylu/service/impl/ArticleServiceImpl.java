package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.boylu.common.RedisConstants;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysCategory;
import com.boylu.entity.SysNotifications;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.mapper.SysCategoryMapper;
import com.boylu.service.ArticleService;
import com.boylu.utils.IpUtil;
import com.boylu.utils.NotificationsUtil;
import com.boylu.utils.PageUtil;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.article.ArchiveListVo;
import com.boylu.vo.article.ArticleDetailVo;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.CategoryListVo;
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

        detailVo.setQuantity(recordRealtimeQuantity(id, detailVo.getQuantity(), userId));
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

    private Integer recordRealtimeQuantity(Long articleId, Integer dbQuantity, Object userId) {
        if (articleId == null) {
            return dbQuantity == null ? 0 : dbQuantity;
        }
        String hashKey = articleId.toString();
        Object cachedValue = redisUtil.hGet(RedisConstants.ARTICLE_QUANTITY, hashKey);
        int currentQuantity = resolveQuantity(dbQuantity, cachedValue);
        List<String> viewerKeys = resolveViewerKeys(userId);
        if (viewerKeys.isEmpty()) {
            return currentQuantity;
        }

        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
        String viewedKey = RedisConstants.ARTICLE_DAILY_VIEWED_USER + hashKey + ":" + today;
        if (hasAnyViewerKey(viewedKey, viewerKeys)) {
            return currentQuantity;
        }

        redisUtil.sAdd(viewedKey, viewerKeys.toArray(new Object[0]));
        redisUtil.expire(viewedKey, RedisConstants.DAY_EXPIRE * 2, TimeUnit.SECONDS);
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
