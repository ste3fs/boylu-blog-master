package com.boylu.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.common.Constants;
import com.boylu.common.RedisConstants;
import com.boylu.common.Result;
import com.boylu.entity.SysNotice;
import com.boylu.mapper.SysNoticeMapper;
import com.boylu.service.HomeService;
import com.boylu.entity.SysWebConfig;
import com.boylu.mapper.SysWebConfigMapper;
import com.boylu.utils.IpUtil;
import com.boylu.utils.RedisUtil;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

    private static final int HOT_SEARCH_TIMEOUT = 8000;
    private static final String HOT_SEARCH_CACHE_KEY_PREFIX = "hot_search:";
    private static final long HOT_SEARCH_CACHE_TTL_MINUTES = 15L;

    private static final Map<String, List<String>> HOT_SEARCH_FALLBACK_URLS;

    static {
        Map<String, List<String>> fallbackUrls = new HashMap<>();
        fallbackUrls.put("weibo", Arrays.asList(
                "https://api.zxz.ee/api/hot/?type=weibo",
                "https://v2.xxapi.cn/api/weibohot"
        ));
        fallbackUrls.put("zhihu", Collections.singletonList("https://api.zxz.ee/api/hot/?type=zhihu"));
        fallbackUrls.put("toutiao", Arrays.asList(
                "https://api.zxz.ee/api/hot/?type=toutiao",
                "https://dabenshi.cn/other/api/hot.php?type=toutiaoHot"
        ));
        fallbackUrls.put("baidu", Collections.singletonList("https://v2.xxapi.cn/api/baiduhot"));
        fallbackUrls.put("csdn", Collections.singletonList("https://v2.xxapi.cn/api/csdnhot"));
        HOT_SEARCH_FALLBACK_URLS = Collections.unmodifiableMap(fallbackUrls);
    }

    private final SysWebConfigMapper sysWebConfigMapper;

    private final RedisUtil redisUtil;

    private final SysNoticeMapper noticeMapper;

    @Value("${hot-search.coderutil.access-key:}")
    private String coderutilAccessKey;

    @Value("${hot-search.coderutil.secret-key:}")
    private String coderutilSecretKey;

    @Override
    public Result<SysWebConfig> getWebConfig() {

        SysWebConfig sysWebConfig = new SysWebConfig();
        Object value = redisUtil.get(RedisConstants.WEB_CONFIG_KEY);
        if (value == null) {
            LambdaQueryWrapper<SysWebConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.last("limit 1");
            sysWebConfig = sysWebConfigMapper.selectOne(wrapper);
        }else {
            sysWebConfig = JSONObject.parseObject(value.toString(), SysWebConfig.class);
        }

        //获取浏览量和访问量
        long blogViewsCount = 0;
        long visitorCount = 0;
        long dailyBlogViewsCount = 0;
        long dailyVisitorCount = 0;
        if (redisUtil.hasKey(RedisConstants.BLOG_VIEWS_COUNT)) {
            blogViewsCount = Long.parseLong(redisUtil.get(RedisConstants.BLOG_VIEWS_COUNT).toString());
        }
        if (redisUtil.hasKey(RedisConstants.UNIQUE_VISITOR_COUNT)) {
            visitorCount = Long.parseLong(redisUtil.get(RedisConstants.UNIQUE_VISITOR_COUNT).toString());
        }
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String dailyVisitorCountKey = RedisConstants.UNIQUE_VISITOR_DAILY + today;
        String dailyViewCountKey = RedisConstants.BLOG_VIEWS_DAILY + today;
        if (redisUtil.hasKey(dailyViewCountKey)) {
            dailyBlogViewsCount = Long.parseLong(redisUtil.get(dailyViewCountKey).toString());
        }
        if (redisUtil.hasKey(dailyVisitorCountKey)) {
            dailyVisitorCount = Long.parseLong(redisUtil.get(dailyVisitorCountKey).toString());
        }

        return Result.success(sysWebConfig)
                .putExtra("blogViewsCount", blogViewsCount)
                .putExtra("visitorCount", visitorCount)
                .putExtra("dailyBlogViewsCount", dailyBlogViewsCount)
                .putExtra("dailyVisitorCount", dailyVisitorCount);
    }

    @Override
    public JSONObject getHotSearch(String type) {
        String normalizedType = normalizeHotSearchType(type);
        String cacheKey = HOT_SEARCH_CACHE_KEY_PREFIX + normalizedType;
        JSONObject cachedResult = getCachedHotSearch(cacheKey);
        if (hasHotSearchData(cachedResult)) {
            cachedResult.put("source", "cache");
            return cachedResult;
        }

        if (StringUtils.hasText(coderutilAccessKey) && StringUtils.hasText(coderutilSecretKey)) {
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("access-key", coderutilAccessKey);
            paramMap.put("secret-key", coderutilSecretKey);
            String url = "https://www.coderutil.com/api/resou/v1/" + normalizedType;
            try {
                JSONObject result = normalizeHotSearchResult(normalizedType, parseHotSearchResponse(HttpUtil.get(url, paramMap)));
                if (hasHotSearchData(result)) {
                    cacheHotSearch(cacheKey, result);
                    return result;
                }
                log.warn("CoderUtil hot search returned empty data, type={}", normalizedType);
            } catch (Exception e) {
                log.warn("Failed to fetch CoderUtil hot search, type={}", normalizedType, e);
            }
        } else {
            log.warn("CoderUtil hot search credentials are not configured, using fallback source.");
        }

        JSONObject fallbackResult = getFallbackHotSearch(normalizedType);
        cacheHotSearch(cacheKey, fallbackResult);
        return fallbackResult;
    }

    private JSONObject getCachedHotSearch(String cacheKey) {
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached == null) {
                return null;
            }
            if (cached instanceof JSONObject) {
                return (JSONObject) cached;
            }
            return JSONObject.parseObject(cached.toString());
        } catch (Exception e) {
            redisUtil.delete(cacheKey);
            log.warn("Failed to parse hot search cache, key={}", cacheKey, e);
            return null;
        }
    }

    private void cacheHotSearch(String cacheKey, JSONObject result) {
        if (!hasHotSearchData(result)) {
            return;
        }
        redisUtil.set(cacheKey, result.toJSONString(), HOT_SEARCH_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private JSONObject getFallbackHotSearch(String type) {
        List<String> urls = HOT_SEARCH_FALLBACK_URLS.get(type);
        if (urls == null || urls.isEmpty()) {
            return emptyHotSearchResult();
        }

        for (String url : urls) {
            try {
                JSONObject result = normalizeHotSearchResult(type, parseHotSearchResponse(HttpUtil.get(url, HOT_SEARCH_TIMEOUT)));
                if (hasHotSearchData(result)) {
                    result.put("source", "fallback");
                    return result;
                }
                log.warn("Fallback hot search returned empty data, type={}, url={}", type, url);
            } catch (Exception e) {
                log.warn("Failed to fetch fallback hot search, type={}, url={}", type, url, e);
            }
        }

        return emptyHotSearchResult();
    }

    private JSONObject parseHotSearchResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return new JSONObject();
        }
        int jsonStart = response.indexOf('{');
        if (jsonStart > 0) {
            response = response.substring(jsonStart);
        }
        return JSONObject.parseObject(response);
    }

    private JSONObject normalizeHotSearchResult(String type, JSONObject source) {
        JSONArray rawList = getHotSearchArray(source);
        JSONArray normalizedList = new JSONArray();
        for (int i = 0; i < rawList.size(); i++) {
            JSONObject rawItem = rawList.getJSONObject(i);
            if (rawItem == null) {
                continue;
            }

            String keyword = firstNonBlank(rawItem, "keyword", "title", "word", "name");
            if (!StringUtils.hasText(keyword)) {
                continue;
            }

            JSONObject item = new JSONObject();
            item.put("keyword", keyword);
            item.put("title", keyword);
            item.put("url", firstNonBlank(rawItem, "url", "link", "mobileUrl", "mobilUrl"));
            if (!StringUtils.hasText(item.getString("url"))) {
                item.put("url", buildHotSearchUrl(type, keyword));
            }
            item.put("summary", firstNonBlank(rawItem, "summary", "desc", "description"));
            item.put("tag", firstNonBlank(rawItem, "tag", "label", "label_desc", "status"));
            item.put("type", type);
            item.put("trend", firstNonBlank(rawItem, "trend", "status"));
            item.put("hotValue", parseHotValue(firstNonBlank(rawItem, "hotValue", "hot", "hot_value", "num", "extra", "viewCount")));
            item.put("rank", rawItem.getOrDefault("rank", rawItem.getOrDefault("index", i + 1)));
            normalizedList.add(item);
        }

        JSONObject result = new JSONObject();
        result.put("data", normalizedList);
        return result;
    }

    private JSONArray getHotSearchArray(JSONObject source) {
        if (source == null) {
            return new JSONArray();
        }

        Object data = source.get("data");
        if (data instanceof JSONArray) {
            return (JSONArray) data;
        }
        if (data instanceof JSONObject) {
            JSONObject dataObject = (JSONObject) data;
            JSONArray nestedData = dataObject.getJSONArray("data");
            if (nestedData != null) {
                return nestedData;
            }
            JSONArray list = dataObject.getJSONArray("list");
            if (list != null) {
                return list;
            }
        }

        JSONArray list = source.getJSONArray("list");
        return list == null ? new JSONArray() : list;
    }

    private boolean hasHotSearchData(JSONObject result) {
        JSONArray data = result == null ? null : result.getJSONArray("data");
        return data != null && !data.isEmpty();
    }

    private String normalizeHotSearchType(String type) {
        if (!StringUtils.hasText(type)) {
            return "weibo";
        }
        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        return HOT_SEARCH_FALLBACK_URLS.containsKey(normalizedType) ? normalizedType : "weibo";
    }

    private String firstNonBlank(JSONObject object, String... fields) {
        for (String field : fields) {
            String value = object.getString(field);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private long parseHotValue(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }

        String normalized = value.replace(",", "").replace(" ", "").trim();
        double multiplier = 1D;
        if (normalized.contains("亿")) {
            multiplier = 100000000D;
        } else if (normalized.contains("万")) {
            multiplier = 10000D;
        }

        String number = normalized.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(number)) {
            return 0L;
        }

        try {
            return Math.round(Double.parseDouble(number) * multiplier);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String buildHotSearchUrl(String type, String keyword) {
        try {
            String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
            if ("weibo".equals(type)) {
                return "https://s.weibo.com/weibo?q=" + encodedKeyword;
            }
            if ("zhihu".equals(type)) {
                return "https://www.zhihu.com/search?type=content&q=" + encodedKeyword;
            }
            if ("toutiao".equals(type)) {
                return "https://so.toutiao.com/search?keyword=" + encodedKeyword;
            }
            if ("csdn".equals(type)) {
                return "https://so.csdn.net/so/search?q=" + encodedKeyword;
            }
            return "https://www.baidu.com/s?wd=" + encodedKeyword;
        } catch (Exception e) {
            return "https://www.baidu.com";
        }
    }

    private JSONObject emptyHotSearchResult() {
        JSONObject result = new JSONObject();
        result.put("data", Collections.emptyList());
        return result;
    }

    @Override
    public void report() {
        // 获取ip
        String ipAddress = IpUtil.getIp();
        // 通过浏览器解析工具类UserAgent获取访问设备信息
        UserAgent userAgent = IpUtil.getUserAgent(Objects.requireNonNull(IpUtil.getRequest()));
        Browser browser = userAgent.getBrowser();
        OperatingSystem operatingSystem = userAgent.getOperatingSystem();
        // 生成唯一用户标识
        String uuid = ipAddress + browser.getName() + operatingSystem.getName();
        String md5 = DigestUtils.md5DigestAsHex(uuid.getBytes());
        // 判断是否访问
        if (!redisUtil.sIsMember(RedisConstants.UNIQUE_VISITOR, md5)) {
            // 访客量+1
            redisUtil.increment(RedisConstants.UNIQUE_VISITOR_COUNT, 1);
            // 保存唯一标识
            redisUtil.sAdd(RedisConstants.UNIQUE_VISITOR, md5);
        }

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String dailyVisitorSetKey = RedisConstants.UNIQUE_VISITOR_DAILY_SET + today;
        String dailyVisitorCountKey = RedisConstants.UNIQUE_VISITOR_DAILY + today;
        String dailyViewCountKey = RedisConstants.BLOG_VIEWS_DAILY + today;

        if (!Boolean.TRUE.equals(redisUtil.sIsMember(dailyVisitorSetKey, md5))) {
            redisUtil.increment(dailyVisitorCountKey, 1);
            redisUtil.sAdd(dailyVisitorSetKey, md5);
            redisUtil.expire(dailyVisitorCountKey, 90, TimeUnit.DAYS);
            redisUtil.expire(dailyVisitorSetKey, 90, TimeUnit.DAYS);
        }

        // 访问量+1
        redisUtil.increment(RedisConstants.BLOG_VIEWS_COUNT, 1);
        redisUtil.increment(dailyViewCountKey, 1);
        redisUtil.expire(dailyViewCountKey, 90, TimeUnit.DAYS);
    }

    @Override
    public Map<String, List<SysNotice>> getNotice() {

        List<SysNotice> sysNotices = noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getIsShow, Constants.YES));
        return sysNotices.stream()
                .collect(Collectors.groupingBy(SysNotice::getPosition));
    }
}
