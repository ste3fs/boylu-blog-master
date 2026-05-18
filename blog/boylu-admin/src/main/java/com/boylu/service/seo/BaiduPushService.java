package com.boylu.service.seo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaiduPushService implements InitializingBean {

    private static final String DEFAULT_HTTPS_ENDPOINT = "https://data.zz.baidu.com/urls";
    private static final String DEFAULT_HTTP_ENDPOINT = "http://data.zz.baidu.com/urls";

    @Value("${seo.baidu.push.enabled:false}")
    private boolean enabled;

    @Value("${seo.baidu.push.endpoint:" + DEFAULT_HTTP_ENDPOINT + "}")
    private String endpoint;

    @Value("${seo.baidu.push.site:https://boylu.cn}")
    private String site;

    @Value("${seo.baidu.push.token:}")
    private String token;

    @Value("${seo.baidu.push.article-path:/article/%d}")
    private String articlePathPattern;

    @Value("${seo.baidu.push.fallback-http-enabled:true}")
    private boolean fallbackHttpEnabled;

    @Value("${seo.baidu.push.connect-timeout-ms:8000}")
    private int connectTimeoutMs;

    @Value("${seo.baidu.push.read-timeout-ms:12000}")
    private int readTimeoutMs;

    @Value("${seo.baidu.push.retry-times:2}")
    private int retryTimes;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void afterPropertiesSet() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(3000, Math.min(connectTimeoutMs, 30000)));
        requestFactory.setReadTimeout(Math.max(5000, Math.min(readTimeoutMs, 60000)));
        restTemplate.setRequestFactory(requestFactory);
    }

    public boolean pushArticleUrl(Long articleId) {
        if (articleId == null || articleId <= 0) {
            return false;
        }
        String articlePath = String.format(articlePathPattern, articleId);
        String articleUrl = site.replaceAll("/+$", "") + articlePath;
        return pushUrls(Collections.singletonList(articleUrl), "article#" + articleId);
    }

    public boolean pushUrls(List<String> urls, String scene) {
        if (!enabled) {
            log.info("Baidu push skipped because feature disabled, scene={}", scene);
            return false;
        }
        if (StringUtils.isBlank(site) || StringUtils.isBlank(token)) {
            log.warn("Baidu push skipped because site/token is empty, scene={}", scene);
            return false;
        }
        if (urls == null || urls.isEmpty()) {
            return false;
        }

        Set<String> normalized = urls.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return false;
        }

        String payload = String.join("\n", normalized);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        List<String> endpoints = resolveCandidateEndpoints();
        int safeRetryTimes = Math.max(1, Math.min(retryTimes, 5));
        String lastError = null;
        for (String candidateEndpoint : endpoints) {
            String pushApi = buildPushApi(candidateEndpoint);
            for (int attempt = 1; attempt <= safeRetryTimes; attempt++) {
                PushAttemptResult result = postToBaidu(pushApi, request, scene, attempt, safeRetryTimes);
                if (result.success) {
                    return true;
                }
                lastError = result.message;
                if (result.permanentFailure) {
                    break;
                }
            }
        }
        log.error("Baidu push failed after all endpoints, scene={}, endpoints={}, lastError={}",
                scene, endpoints, lastError);
        return false;
    }

    private PushAttemptResult postToBaidu(String pushApi, HttpEntity<String> request, String scene,
                                          int attempt, int maxAttempts) {
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(pushApi, request, String.class);
            String body = StringUtils.defaultString(resp.getBody());
            if (!resp.getStatusCode().is2xxSuccessful()) {
                String message = "HTTP " + resp.getStatusCodeValue() + ": " + body;
                log.warn("Baidu push failed, scene={}, attempt={}/{}, api={}, response={}",
                        scene, attempt, maxAttempts, maskToken(pushApi), message);
                return new PushAttemptResult(false, false, message);
            }
            if (isBaiduErrorBody(body)) {
                log.warn("Baidu push rejected, scene={}, attempt={}/{}, api={}, response={}",
                        scene, attempt, maxAttempts, maskToken(pushApi), body);
                return new PushAttemptResult(false, true, body);
            }
            log.info("Baidu push done, scene={}, attempt={}/{}, api={}, status={}, response={}",
                    scene, attempt, maxAttempts, maskToken(pushApi), resp.getStatusCodeValue(), body);
            return new PushAttemptResult(true, false, body);
        } catch (HttpStatusCodeException ex) {
            String body = StringUtils.defaultString(ex.getResponseBodyAsString());
            String message = "HTTP " + ex.getRawStatusCode() + ": " + body;
            log.warn("Baidu push HTTP failed, scene={}, attempt={}/{}, api={}, response={}",
                    scene, attempt, maxAttempts, maskToken(pushApi), message);
            return new PushAttemptResult(false, ex.getRawStatusCode() >= 400 && ex.getRawStatusCode() < 500, message);
        } catch (Exception ex) {
            log.warn("Baidu push request failed, scene={}, attempt={}/{}, api={}, error={}",
                    scene, attempt, maxAttempts, maskToken(pushApi), ex.getMessage());
            return new PushAttemptResult(false, false, ex.getMessage());
        }
    }

    private List<String> resolveCandidateEndpoints() {
        LinkedHashSet<String> endpoints = new LinkedHashSet<>();
        addEndpoint(endpoints, endpoint);
        addEndpoint(endpoints, DEFAULT_HTTPS_ENDPOINT);
        if (fallbackHttpEnabled) {
            addEndpoint(endpoints, toHttpEndpoint(endpoint));
            addEndpoint(endpoints, DEFAULT_HTTP_ENDPOINT);
        }
        return new ArrayList<>(endpoints);
    }

    private void addEndpoint(Set<String> endpoints, String value) {
        String normalized = StringUtils.defaultString(value).trim().replaceAll("/+$", "");
        if (StringUtils.isNotBlank(normalized)) {
            endpoints.add(normalized);
        }
    }

    private String toHttpEndpoint(String value) {
        String normalized = StringUtils.defaultString(value).trim();
        if (StringUtils.startsWithIgnoreCase(normalized, "https://")) {
            return "http://" + normalized.substring("https://".length());
        }
        return normalized;
    }

    private String buildPushApi(String baseEndpoint) {
        String separator = StringUtils.contains(baseEndpoint, "?") ? "&" : "?";
        return baseEndpoint
                + separator
                + "site=" + urlEncode(site.replaceAll("^https?://", "").replaceAll("/+$", ""))
                + "&token=" + urlEncode(token.trim());
    }

    private boolean isBaiduErrorBody(String body) {
        return StringUtils.contains(StringUtils.defaultString(body), "\"error\"")
                || StringUtils.containsIgnoreCase(StringUtils.defaultString(body), "not valid")
                || StringUtils.containsIgnoreCase(StringUtils.defaultString(body), "token");
    }

    private String maskToken(String url) {
        return StringUtils.defaultString(url).replaceAll("([?&]token=)[^&]+", "$1***");
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not supported", e);
        }
    }

    private static class PushAttemptResult {
        private final boolean success;
        private final boolean permanentFailure;
        private final String message;

        private PushAttemptResult(boolean success, boolean permanentFailure, String message) {
            this.success = success;
            this.permanentFailure = permanentFailure;
            this.message = message;
        }
    }
}
