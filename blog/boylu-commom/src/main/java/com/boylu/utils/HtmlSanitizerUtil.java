package com.boylu.utils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cleans user supplied rich text before it is persisted or rendered.
 */
public final class HtmlSanitizerUtil {

    private static final String LOCAL_IMAGE_PLACEHOLDER_PREFIX = "https://boylu.local/__local-image__/";

    private static final Safelist USER_RICH_TEXT_SAFELIST = Safelist.relaxed()
            .addTags("span", "mark", "mention")
            .addAttributes(":all", "class")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "src", "alt", "title", "class", "data-origin")
            .addProtocols("a", "href", "http", "https", "mailto", "tel")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    private HtmlSanitizerUtil() {
    }

    public static String sanitizeUserRichText(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }

        String preparedHtml = protectLocalImageSources(html);
        String cleanedHtml = Jsoup.clean(preparedHtml, "", USER_RICH_TEXT_SAFELIST, OUTPUT_SETTINGS).trim();
        return restoreLocalImageSources(cleanedHtml);
    }

    private static String protectLocalImageSources(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Elements images = document.select("img[src]");
        for (Element image : images) {
            String source = StringUtils.trimToEmpty(image.attr("src"));
            if (isAllowedLocalImageSource(source)) {
                image.attr("src", LOCAL_IMAGE_PLACEHOLDER_PREFIX + encodeBase64Url(source));
            }
        }
        return document.body().html();
    }

    private static String restoreLocalImageSources(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Elements images = document.select("img[src]");
        for (Element image : images) {
            String source = image.attr("src");
            if (!source.startsWith(LOCAL_IMAGE_PLACEHOLDER_PREFIX)) {
                continue;
            }
            String encoded = source.substring(LOCAL_IMAGE_PLACEHOLDER_PREFIX.length());
            String restored = decodeBase64Url(encoded);
            if (isAllowedLocalImageSource(restored)) {
                image.attr("src", restored);
            }
        }
        return document.body().html().trim();
    }

    private static boolean isAllowedLocalImageSource(String source) {
        if (StringUtils.isBlank(source) || source.contains("\n") || source.contains("\r")) {
            return false;
        }
        String value = source.trim();
        return value.startsWith("/localFile/")
                || value.startsWith("/img/local/")
                || value.startsWith("/file/content/")
                || value.startsWith("/file/view/")
                || value.startsWith("/boylu/file/content/")
                || value.startsWith("/boylu/file/view/")
                || value.startsWith("/mojian/file/content/")
                || value.startsWith("/mojian/file/view/");
    }

    private static String encodeBase64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBase64Url(String value) {
        try {
            int padding = (4 - value.length() % 4) % 4;
            String padded = value + "====".substring(0, padding);
            return new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
