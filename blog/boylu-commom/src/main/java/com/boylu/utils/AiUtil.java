package com.boylu.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.common.AiConfigKeys;
import com.boylu.entity.SysConfig;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysConfigMapper;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiUtil {

    private static final String DEFAULT_COMPATIBLE_MODEL = "gpt-4o-mini";
    private static final Duration COMPATIBLE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration ARK_TIMEOUT = Duration.ofMinutes(5);

    private final SysConfigMapper sysConfigMapper;

    @Value("${ai.apiKey:}")
    private String apiKey;

    @Value("${ai.baseUrl:}")
    private String baseUrl;

    @Value("${ai.model:}")
    private String model;

    private volatile ArkService arkService;
    private volatile String arkServiceSignature;

    public String getModel() {
        RuntimeConfig runtimeConfig = resolveRuntimeConfig();
        if (StringUtils.isNotBlank(runtimeConfig.getModel())) {
            return runtimeConfig.getModel();
        }
        return useCompatibleProvider(runtimeConfig.getBaseUrl()) ? DEFAULT_COMPATIBLE_MODEL : StringUtils.defaultString(model);
    }

    public String send(String content) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(StringUtils.defaultString(content))
                .build());
        return send(messages);
    }

    public String send(List<ChatMessage> messages) {
        List<ChatMessage> safeMessages = new ArrayList<>();
        if (messages != null) {
            messages.stream()
                    .filter(Objects::nonNull)
                    .forEach(safeMessages::add);
        }
        if (safeMessages.isEmpty()) {
            safeMessages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content("")
                    .build());
        }

        RuntimeConfig runtimeConfig = resolveRuntimeConfig();
        validate(runtimeConfig);

        if (useCompatibleProvider(runtimeConfig.getBaseUrl())) {
            return sendByCompatibleProvider(safeMessages, runtimeConfig);
        }
        return sendByArk(safeMessages, runtimeConfig);
    }

    private String sendByArk(List<ChatMessage> messages, RuntimeConfig config) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .build();

        try {
            return service(config)
                    .createChatCompletion(request)
                    .getChoices()
                    .stream()
                    .map(choice -> choice.getMessage() == null ? null : choice.getMessage().getContent())
                    .filter(content -> StringUtils.isNotBlank(String.valueOf(content)))
                    .findFirst()
                    .map(String::valueOf)
                    .orElseThrow(() -> new ServiceException("AI 暂时没有返回内容，请稍后再试"));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ark AI request failed", e);
            throw new ServiceException("AI 请求失败，请稍后再试");
        }
    }

    private String sendByCompatibleProvider(List<ChatMessage> messages, RuntimeConfig config) {
        List<String> endpoints = buildCompatibleEndpoints(config.getBaseUrl());
        String lastErrorMessage = "AI 请求失败，请稍后再试";

        for (int index = 0; index < endpoints.size(); index++) {
            String endpoint = endpoints.get(index);
            HttpResponse response = null;
            try {
                response = HttpRequest.post(endpoint)
                        .timeout(Math.toIntExact(COMPATIBLE_TIMEOUT.toMillis()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.getApiKey())
                        .body(buildCompatibleRequest(messages, config).toString())
                        .execute();

                String responseBody = StringUtils.defaultString(response.body());
                if (response.getStatus() >= 200 && response.getStatus() < 300) {
                    String content = extractCompatibleContent(responseBody);
                    if (StringUtils.isBlank(content)) {
                        throw new ServiceException("AI 暂时没有返回内容，请稍后再试");
                    }
                    return content;
                }

                lastErrorMessage = extractCompatibleError(responseBody);
                if (response.getStatus() == 404 && index < endpoints.size() - 1) {
                    continue;
                }
            } catch (ServiceException e) {
                if (index >= endpoints.size() - 1) {
                    throw e;
                }
                lastErrorMessage = e.getMessage();
            } catch (Exception e) {
                log.error("Compatible AI request failed: {}", endpoint, e);
                lastErrorMessage = "AI 请求失败，请稍后再试";
                if (index >= endpoints.size() - 1) {
                    throw new ServiceException(lastErrorMessage);
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        throw new ServiceException(StringUtils.defaultIfBlank(lastErrorMessage, "AI 请求失败，请稍后再试"));
    }

    private JSONObject buildCompatibleRequest(List<ChatMessage> messages, RuntimeConfig config) {
        JSONObject body = new JSONObject();
        body.set("model", StringUtils.defaultIfBlank(config.getModel(), DEFAULT_COMPATIBLE_MODEL));
        body.set("stream", false);
        body.set("messages", toCompatibleMessages(messages));
        return body;
    }

    private JSONArray toCompatibleMessages(List<ChatMessage> messages) {
        JSONArray result = new JSONArray();
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.set("role", resolveCompatibleRole(message.getRole()));
            item.set("content", resolveMessageContent(message));
            result.add(item);
        }
        return result;
    }

    private String resolveCompatibleRole(ChatMessageRole role) {
        if (role == null) {
            return "user";
        }
        if (role == ChatMessageRole.SYSTEM) {
            return "system";
        }
        if (role == ChatMessageRole.ASSISTANT) {
            return "assistant";
        }
        return "user";
    }

    private String resolveMessageContent(ChatMessage message) {
        Object content = message.getContent();
        if (content == null) {
            return "";
        }
        if (content instanceof String) {
            return (String) content;
        }
        return String.valueOf(content);
    }

    private String extractCompatibleContent(String rawBody) {
        JSONObject body = JSONUtil.parseObj(rawBody);
        JSONArray choices = body.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            return "";
        }
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        if (content instanceof JSONArray) {
            return flattenContentArray((JSONArray) content);
        }
        return StringUtils.trimToEmpty(String.valueOf(content));
    }

    private String flattenContentArray(JSONArray contentArray) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < contentArray.size(); i++) {
            Object item = contentArray.get(i);
            if (item instanceof JSONObject) {
                JSONObject json = (JSONObject) item;
                String text = json.getStr("text");
                if (StringUtils.isBlank(text)) {
                    text = json.getStr("content");
                }
                if (StringUtils.isNotBlank(text)) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text.trim());
                }
            } else if (item != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(String.valueOf(item).trim());
            }
        }
        return builder.toString().trim();
    }

    private String extractCompatibleError(String rawBody) {
        try {
            JSONObject body = JSONUtil.parseObj(rawBody);
            JSONObject error = body.getJSONObject("error");
            if (error != null && StringUtils.isNotBlank(error.getStr("message"))) {
                return error.getStr("message");
            }
            if (StringUtils.isNotBlank(body.getStr("message"))) {
                return body.getStr("message");
            }
        } catch (Exception ignored) {
        }
        return StringUtils.defaultIfBlank(StringUtils.trimToEmpty(rawBody), "AI 请求失败，请稍后再试");
    }

    private List<String> buildCompatibleEndpoints(String rawBaseUrl) {
        String normalized = StringUtils.removeEnd(StringUtils.trimToEmpty(rawBaseUrl), "/");
        Set<String> endpoints = new LinkedHashSet<>();
        if (StringUtils.isBlank(normalized)) {
            return new ArrayList<>(endpoints);
        }

        String lowerBase = normalized.toLowerCase(Locale.ROOT);
        if (lowerBase.endsWith("/v1")) {
            endpoints.add(normalized + "/chat/completions");
        } else {
            endpoints.add(normalized + "/v1/chat/completions");
            endpoints.add(normalized + "/chat/completions");
        }
        return new ArrayList<>(endpoints);
    }

    private ArkService service(RuntimeConfig config) {
        String signature = StringUtils.defaultString(config.getApiKey()) + "|" + StringUtils.defaultString(config.getBaseUrl());
        if (arkService == null || !StringUtils.equals(signature, arkServiceSignature)) {
            synchronized (this) {
                if (arkService == null || !StringUtils.equals(signature, arkServiceSignature)) {
                    arkService = ArkService.builder()
                            .apiKey(config.getApiKey())
                            .timeout(ARK_TIMEOUT)
                            .baseUrl(config.getBaseUrl())
                            .build();
                    arkServiceSignature = signature;
                }
            }
        }
        return arkService;
    }

    private RuntimeConfig resolveRuntimeConfig() {
        return new RuntimeConfig(
                readConfig(AiConfigKeys.RUNTIME_BASE_URL, baseUrl),
                readConfig(AiConfigKeys.RUNTIME_API_KEY, apiKey),
                StringUtils.defaultIfBlank(readConfig(AiConfigKeys.RUNTIME_MODEL, model), DEFAULT_COMPATIBLE_MODEL)
        );
    }

    private String readConfig(String key, String fallback) {
        try {
            SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, key)
                    .last("limit 1"));
            if (config != null && StringUtils.isNotBlank(config.getConfigValue())) {
                return config.getConfigValue().trim();
            }
        } catch (Exception e) {
            log.warn("Read AI runtime config failed: {}", key, e);
        }
        return StringUtils.trimToEmpty(fallback);
    }

    private void validate(RuntimeConfig config) {
        if (StringUtils.isBlank(config.getApiKey())) {
            throw new ServiceException("AI API Key 未配置");
        }
        if (StringUtils.isBlank(config.getBaseUrl())) {
            throw new ServiceException("AI Base URL 未配置");
        }
        if (StringUtils.isBlank(config.getModel())) {
            throw new ServiceException("AI 模型未配置");
        }
    }

    private boolean useCompatibleProvider(String rawBaseUrl) {
        String normalized = StringUtils.trimToEmpty(rawBaseUrl).toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("http")) {
            return false;
        }
        return !normalized.contains("volces")
                && !normalized.contains("volcengine")
                && !normalized.contains("ark");
    }

    @Data
    @AllArgsConstructor
    public static class RuntimeConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
    }
}
