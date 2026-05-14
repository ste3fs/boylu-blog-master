package com.boylu.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Normalizes local file URLs stored in rich text content.
 *
 * <p>Older article HTML may contain repeated namespace prefixes such as
 * /boylu/boylu/file/content/{id}. Normalize those values before persisting
 * or returning article content so old data and new edits use the same stable
 * public file entry.</p>
 */
public final class LocalFileUrlNormalizeUtil {

    private static final String LOCAL_FILE_SEGMENT = "/localFile/";

    private static final Pattern ABSOLUTE_LOCAL_FILE_PREFIX = Pattern.compile(
            "https?://[^\"'\\s)]+/localFile/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX = Pattern.compile(
            "//[^\"'\\s)]+/localFile/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ABSOLUTE_FILE_VIEW_PREFIX = Pattern.compile(
            "https?://[^\"'\\s)]+(?:/(?:boylu|mojian))?/file/(?:view|content)/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROTOCOL_RELATIVE_FILE_VIEW_PREFIX = Pattern.compile(
            "//[^\"'\\s)]+(?:/(?:boylu|mojian))?/file/(?:view|content)/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DUPLICATED_NAMESPACE_FILE_PREFIX = Pattern.compile(
            "(?:/(?:boylu|mojian))+(/file/(?:view|content)/)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MOJIAN_FILE_PREFIX = Pattern.compile(
            "/mojian/file/(?:view|content)/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BOYLU_FILE_VIEW_PREFIX = Pattern.compile(
            "/boylu/file/view/",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROOT_FILE_PREFIX = Pattern.compile(
            "([\"'\\s(=])/?file/(?:view|content)/",
            Pattern.CASE_INSENSITIVE
    );

    private LocalFileUrlNormalizeUtil() {
    }

    public static String normalizeText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }

        String normalized = text;
        normalized = ABSOLUTE_LOCAL_FILE_PREFIX.matcher(normalized).replaceAll(LOCAL_FILE_SEGMENT);
        normalized = PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX.matcher(normalized).replaceAll(LOCAL_FILE_SEGMENT);
        normalized = ABSOLUTE_FILE_VIEW_PREFIX.matcher(normalized).replaceAll("/boylu/file/content/");
        normalized = PROTOCOL_RELATIVE_FILE_VIEW_PREFIX.matcher(normalized).replaceAll("/boylu/file/content/");
        normalized = DUPLICATED_NAMESPACE_FILE_PREFIX.matcher(normalized).replaceAll("/boylu$1");
        normalized = BOYLU_FILE_VIEW_PREFIX.matcher(normalized).replaceAll("/boylu/file/content/");
        normalized = MOJIAN_FILE_PREFIX.matcher(normalized).replaceAll("/boylu/file/content/");
        normalized = ROOT_FILE_PREFIX.matcher(normalized).replaceAll("$1/boylu/file/content/");
        return normalized;
    }

    public static String normalizeUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }

        String normalized = normalizeText(url.trim());
        int localIndex = normalized.indexOf(LOCAL_FILE_SEGMENT);
        if (localIndex >= 0) {
            return normalized.substring(localIndex);
        }

        int fileIndex = normalized.indexOf("/boylu/file/content/");
        if (fileIndex >= 0) {
            return normalized.substring(fileIndex);
        }

        return normalized;
    }
}
