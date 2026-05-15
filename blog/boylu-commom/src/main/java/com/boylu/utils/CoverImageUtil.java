package com.boylu.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.boylu.vo.article.CoverImageVo;
import org.apache.commons.lang3.StringUtils;

public final class CoverImageUtil {

    public static final String FALLBACK_IMAGE = "/images/boylu-image-loading-fallback.webp";
    public static final String DEFAULT_DOMINANT_COLOR = "#eef4ff";
    public static final int DEFAULT_WIDTH = 1600;
    public static final int DEFAULT_HEIGHT = 900;

    private CoverImageUtil() {
    }

    public static CoverImageVo fromJson(String coverImageJson, String legacyCover, String alt) {
        CoverImageVo coverImage = null;
        if (StringUtils.isNotBlank(coverImageJson)) {
            try {
                coverImage = JSON.parseObject(coverImageJson, CoverImageVo.class);
            } catch (Exception ignored) {
                coverImage = null;
            }
        }

        if (coverImage == null) {
            coverImage = new CoverImageVo();
        }

        String normalizedLegacyCover = LocalFileUrlNormalizeUtil.normalizeUrl(legacyCover);

        if (StringUtils.isBlank(coverImage.getAlt())) {
            coverImage.setAlt(StringUtils.defaultString(alt));
        }
        if (coverImage.getWidth() == null || coverImage.getWidth() <= 0) {
            coverImage.setWidth(DEFAULT_WIDTH);
        }
        if (coverImage.getHeight() == null || coverImage.getHeight() <= 0) {
            coverImage.setHeight(DEFAULT_HEIGHT);
        }
        if (StringUtils.isBlank(coverImage.getDominantColor())) {
            coverImage.setDominantColor(DEFAULT_DOMINANT_COLOR);
        }
        if (StringUtils.isBlank(coverImage.getFallback())) {
            coverImage.setFallback(StringUtils.defaultIfBlank(normalizedLegacyCover, FALLBACK_IMAGE));
        } else {
            coverImage.setFallback(LocalFileUrlNormalizeUtil.normalizeUrl(coverImage.getFallback()));
        }
        if (StringUtils.isBlank(coverImage.getKey()) && StringUtils.isNotBlank(normalizedLegacyCover)) {
            coverImage.setKey(normalizedLegacyCover);
        }

        return coverImage;
    }

    public static String toJson(CoverImageVo coverImage) {
        if (coverImage == null) {
            return null;
        }
        return JSON.toJSONString(coverImage, JSONWriter.Feature.WriteNonStringKeyAsString);
    }
}
