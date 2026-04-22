package com.mojian.controller.site;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.mojian.common.AiConfigKeys;
import com.mojian.common.Result;
import com.mojian.dto.ai.AiRuntimeConfigDto;
import com.mojian.dto.ai.AiTrainingChatDto;
import com.mojian.dto.ai.AiTrainingConfigDto;
import com.mojian.entity.SysConfig;
import com.mojian.exception.ServiceException;
import com.mojian.service.SysConfigService;
import com.mojian.service.ai.AiTrainingStudioService;
import com.mojian.vo.ai.AiAdminConfigVo;
import com.mojian.vo.ai.AiTrainingChatVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/sys/ai")
@RequiredArgsConstructor
@Api(tags = "后台-AI配置")
public class SysAiConfigController {

    private static final String DEFAULT_PERSONA_RESOURCE = "ai/persona-boylu.txt";

    private final SysConfigService sysConfigService;
    private final AiTrainingStudioService aiTrainingStudioService;

    @Value("${ai.baseUrl:}")
    private String defaultBaseUrl;

    @Value("${ai.apiKey:}")
    private String defaultApiKey;

    @Value("${ai.model:}")
    private String defaultModel;

    @Value("${ai.personaPromptPath:}")
    private String personaPromptPath;

    @GetMapping("/config")
    @ApiOperation(value = "获取 AI 配置")
    @SaCheckPermission("sys:web:update")
    public Result<AiAdminConfigVo> getConfig() {
        AiAdminConfigVo vo = new AiAdminConfigVo();
        vo.setPersonaPromptPath(resolvePersonaPromptPathLabel());
        vo.setPersonaPrompt(loadPersonaPrompt());
        vo.setBaseUrl(readConfig(AiConfigKeys.RUNTIME_BASE_URL, defaultBaseUrl));
        vo.setApiKeyMasked(maskApiKey(readConfig(AiConfigKeys.RUNTIME_API_KEY, defaultApiKey)));
        vo.setModel(readConfig(AiConfigKeys.RUNTIME_MODEL, defaultModel));
        vo.setTrainingPrompt(readConfig(AiConfigKeys.TRAINING_PROMPT, ""));
        vo.setTrainingEnabled(StringUtils.equalsIgnoreCase(readConfig(AiConfigKeys.TRAINING_ENABLED, "Y"), "Y"));
        return Result.success(vo);
    }

    @PutMapping("/config/runtime")
    @ApiOperation(value = "更新 AI 运行配置")
    @SaCheckPermission("sys:web:update")
    public Result<Void> updateRuntimeConfig(@RequestBody AiRuntimeConfigDto dto) {
        if (dto == null) {
            throw new ServiceException("运行配置不能为空");
        }

        String baseUrl = StringUtils.trimToEmpty(dto.getBaseUrl());
        String model = StringUtils.trimToEmpty(dto.getModel());
        String apiKey = StringUtils.trimToEmpty(dto.getApiKey());

        if (StringUtils.isBlank(baseUrl)) {
            throw new ServiceException("Base URL 不能为空");
        }
        if (StringUtils.isBlank(model)) {
            throw new ServiceException("模型不能为空");
        }
        if (StringUtils.isBlank(apiKey)) {
            apiKey = readConfig(AiConfigKeys.RUNTIME_API_KEY, defaultApiKey);
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new ServiceException("API Key 不能为空");
        }

        upsertConfig(AiConfigKeys.RUNTIME_BASE_URL, "AI 运行 Base URL", baseUrl, "AI 运行时 Base URL");
        upsertConfig(AiConfigKeys.RUNTIME_MODEL, "AI 运行模型", model, "AI 运行时模型");
        upsertConfig(AiConfigKeys.RUNTIME_API_KEY, "AI 运行 API Key", apiKey, "AI 运行时 API Key");
        return Result.success();
    }

    @PutMapping("/config/training")
    @ApiOperation(value = "更新 AI 训练配置")
    @SaCheckPermission("sys:web:update")
    public Result<Void> updateTrainingConfig(@RequestBody AiTrainingConfigDto dto) {
        if (dto == null) {
            throw new ServiceException("训练配置不能为空");
        }
        upsertConfig(
                AiConfigKeys.TRAINING_PROMPT,
                "AI 训练提示词",
                StringUtils.defaultString(dto.getTrainingPrompt()),
                "AI 训练提示词"
        );
        upsertConfig(
                AiConfigKeys.TRAINING_ENABLED,
                "AI 训练开关",
                Boolean.TRUE.equals(dto.getTrainingEnabled()) ? "Y" : "N",
                "AI 训练是否启用"
        );
        return Result.success();
    }

    @PostMapping("/config/training/chat")
    @ApiOperation(value = "对话式训练")
    @SaCheckPermission("sys:web:update")
    public Result<AiTrainingChatVo> chatTraining(@RequestBody(required = false) AiTrainingChatDto dto) {
        return Result.success(aiTrainingStudioService.chat(dto));
    }

    private void upsertConfig(String key, String name, String value, String remark) {
        SysConfig existing = sysConfigService.selectConfigByKey(key);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigName(name);
            config.setConfigValue(value);
            config.setConfigType("N");
            config.setRemark(remark);
            sysConfigService.insert(config);
            return;
        }
        existing.setConfigValue(value);
        existing.setConfigName(name);
        existing.setRemark(remark);
        sysConfigService.update(existing);
    }

    private String readConfig(String key, String fallback) {
        SysConfig config = sysConfigService.selectConfigByKey(key);
        if (config != null && StringUtils.isNotBlank(config.getConfigValue())) {
            return config.getConfigValue().trim();
        }
        return StringUtils.trimToEmpty(fallback);
    }

    private String resolvePersonaPromptPathLabel() {
        if (StringUtils.isNotBlank(personaPromptPath)) {
            return personaPromptPath.trim();
        }
        return "classpath:" + DEFAULT_PERSONA_RESOURCE;
    }

    private String loadPersonaPrompt() {
        try {
            if (StringUtils.isNotBlank(personaPromptPath) && FileUtil.exist(personaPromptPath)) {
                return FileUtil.readString(personaPromptPath, StandardCharsets.UTF_8);
            }

            ClassPathResource resource = new ClassPathResource(DEFAULT_PERSONA_RESOURCE);
            if (!resource.exists()) {
                return "";
            }
            try (InputStream inputStream = resource.getInputStream()) {
                return IoUtil.read(inputStream, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String maskApiKey(String rawApiKey) {
        String normalized = StringUtils.trimToEmpty(rawApiKey);
        if (StringUtils.isBlank(normalized)) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized;
        }
        return normalized.substring(0, 4) + "****" + normalized.substring(normalized.length() - 4);
    }
}
