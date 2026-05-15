package com.boylu.quartz;

import com.boylu.common.RedisConstants;
import com.boylu.entity.SysArticle;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component("task")
@RequiredArgsConstructor
@Slf4j
public class TaskQuartz {

    private final RedisUtil redisUtil;

    private final SysArticleMapper articleMapper;

    public void neatMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        // no-op
    }

    public void neatParams(String params) {
        log.debug("execute params method: {}", params);
    }

    public void neatNoParams() {
        log.debug("execute no params method");
    }

    /**
     * 定时同步阅读量
     */
    public void syncQuantity() {
        Map<Object, Object> map = redisUtil.hGetAll(RedisConstants.ARTICLE_QUANTITY);
        if (map == null || map.isEmpty()) {
            return;
        }

        List<SysArticle> articles = new ArrayList<SysArticle>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            int quantity = parseQuantity(entry.getValue());
            articles.add(SysArticle.builder()
                    .id(Long.parseLong(entry.getKey().toString()))
                    .quantity(quantity)
                    .build());
        }

        if (!articles.isEmpty()) {
            articleMapper.updateBatchQuantity(articles);
        }
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
}
