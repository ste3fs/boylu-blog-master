package com.boylu.service.seo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaiduPushService {

    @Value("${seo.baidu.push.enabled:false}")
    private boolean enabled;

    @Value("${seo.baidu.push.endpoint:https://data.zz.baidu.com/urls}")
    private String endpoint;

    @Value("${seo.baidu.push.site:https://boylu.cn}")
    private String site;

    @Value("${seo.baidu.push.token:}")
    private String token;

    @Value("${seo.baidu.push.article-path:/article/%d}")
    private String articlePathPattern;

    private final RestTemplate restTemplate = new RestTemplate();

    public void pushArticleUrl(Long articleId) {
        if (articleId == null || articleId <= 0) {
            return;
        }
        String articlePath = String.format(articlePathPattern, articleId);
        String articleUrl = site.replaceAll("/+$", "") + articlePath;
        pushUrls(Collections.singletonList(articleUrl), "article#" + articleId);
    }

    public void pushUrls(List<String> urls, String scene) {
        if (!enabled) {
            log.info("Baidu push skipped because feature disabled, scene={}", scene);
            return;
        }
        if (StringUtils.isBlank(site) || StringUtils.isBlank(token)) {
            log.warn("Baidu push skipped because site/token is empty, scene={}", scene);
            return;
        }
        if (urls == null || urls.isEmpty()) {
            return;
        }

        Set<String> normalized = urls.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return;
        }

        String pushApi = endpoint
                + "?site=" + urlEncode(site.replaceAll("^https?://", "").replaceAll("/+$", ""))
                + "&token=" + urlEncode(token.trim());
        String payload = String.join("\n", normalized);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(pushApi, request, String.class);
            log.info("Baidu push done, scene={}, status={}, response={}",
                    scene, resp.getStatusCodeValue(), StringUtils.defaultString(resp.getBody()));
        } catch (Exception ex) {
            log.error("Baidu push failed, scene={}, error={}", scene, ex.getMessage(), ex);
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not supported", e);
        }
    }
}
