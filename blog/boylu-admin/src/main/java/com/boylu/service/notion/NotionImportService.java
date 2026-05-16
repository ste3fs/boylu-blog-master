package com.boylu.service.notion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.dto.article.NotionImportDto;
import com.boylu.entity.SysArticle;
import com.boylu.entity.FileDetail;
import com.boylu.exception.ServiceException;
import com.boylu.service.FileDetailService;
import com.boylu.utils.CoverImageUtil;
import com.boylu.utils.DateUtil;
import com.boylu.vo.article.SysArticleDetailVo;
import com.boylu.vo.article.CoverImageVo;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.Proxy;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionImportService {

    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String DEFAULT_PUBLIC_FILE_CONTENT_PREFIX = "/boylu/file/content/";
    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
    private static final Pattern COMPACT_UUID_PATTERN = Pattern.compile("(?i)([0-9a-f]{32})");
    private static final Pattern MARKDOWN_IMAGE_URL_PATTERN = Pattern.compile("!\\[[^\\]]*]\\((https?://[^\\s)]+)\\)");
    private static final Pattern HTML_IMAGE_SRC_PATTERN = Pattern.compile("(<img\\b[^>]*?\\bsrc=[\"'])(https?://[^\"']+)([\"'][^>]*>)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_BLOCK_DEPTH = 8;
    private static final int MAX_BLOCK_COUNT = 600;
    private static final int NOTION_REQUEST_TIMEOUT_MS = 15000;
    private static final int NOTION_REQUEST_RETRY_TIMES = 4;
    private static final long NOTION_REQUEST_RETRY_DELAY_MS = 1000L;

    private final FileStorageService fileStorageService;
    private final FileDetailService fileDetailService;

    @Value("${notion.api-token:}")
    private String apiToken;

    @Value("${notion.version:2022-06-28}")
    private String notionVersion;

    @Value("${notion.import-images:true}")
    private boolean defaultImportImages;

    @Value("${notion.max-image-size-mb:10}")
    private long maxImageSizeMb;

    @Value("${app.file.public-prefix:" + DEFAULT_PUBLIC_FILE_CONTENT_PREFIX + "}")
    private String publicFileContentPrefix;

    public ImportPageResult importPage(NotionImportDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getPageUrl())) {
            throw new ServiceException("请填写 Notion 页面地址或 Page ID");
        }
        if (StringUtils.isBlank(apiToken)) {
            throw new ServiceException("后端未配置 NOTION_API_TOKEN，无法读取 Notion 页面");
        }

        String pageId = extractPageId(dto.getPageUrl());
        JSONObject page = requestNotionObjectWithRetry("/pages/" + urlEncode(pageId));

        List<String> warnings = new ArrayList<>();
        ImportContext context = new ImportContext(resolveImportImages(dto), warnings);
        String title = StringUtils.defaultIfBlank(extractPageTitle(page), "Notion 笔记 " + pageId);

        StringBuilder markdown = new StringBuilder();
        StringBuilder plainText = new StringBuilder();
        renderChildren(pageId, 0, markdown, plainText, context);

        if (StringUtils.isBlank(markdown.toString())) {
            markdown.append("> Notion 页面没有可导入的正文内容。\n");
            warnings.add("Notion 页面正文为空，已生成空白草稿。");
        }

        String markdownContent = markdown.toString().trim();
        SysArticleDetailVo article = new SysArticleDetailVo();
        article.setTitle(title);
        article.setCategoryName(StringUtils.defaultIfBlank(dto.getCategoryName(), "Notion"));
        article.setTags(normalizeTags(dto.getTags()));
        article.setCover(resolveCover(dto, page, context));
        article.setSummary(StringUtils.defaultIfBlank(dto.getSummary(), buildSummary(plainText.toString())));
        article.setKeywords(StringUtils.defaultIfBlank(dto.getKeywords(), String.join(",", article.getTags())));
        article.setContentMd(markdownContent);
        article.setContent(renderMarkdown(markdownContent));
        article.setReadType(defaultInt(dto.getReadType(), 1));
        article.setStatus(defaultInt(dto.getStatus(), 0));
        article.setIsOriginal(defaultInt(dto.getIsOriginal(), 1));
        article.setOriginalUrl(dto.getPageUrl().trim());
        article.setIsStick(defaultInt(dto.getIsStick(), 0));
        article.setIsCarousel(defaultInt(dto.getIsCarousel(), 0));
        article.setIsRecommend(defaultInt(dto.getIsRecommend(), 0));

        return new ImportPageResult(article, context.getImportedBlocks(), warnings);
    }

    public String extractPageIdSafely(String value) {
        try {
            return extractPageId(value);
        } catch (Exception ex) {
            return null;
        }
    }

    public LocalizeImagesResult localizeArticleImages(SysArticle article) {
        if (article == null || article.getId() == null) {
            return new LocalizeImagesResult(null, 0, Collections.emptyList());
        }
        List<String> warnings = new ArrayList<>();
        ImportContext context = new ImportContext(true, warnings);
        SysArticle update = new SysArticle();
        update.setId(article.getId());
        int changedCount = 0;

        String localizedContentMd = replaceMarkdownImages(article.getContentMd(), context);
        if (!StringUtils.equals(localizedContentMd, article.getContentMd())) {
            update.setContentMd(localizedContentMd);
            changedCount++;
        }

        String localizedContent = replaceHtmlImages(article.getContent(), context);
        if (!StringUtils.equals(localizedContent, article.getContent())) {
            update.setContent(localizedContent);
            changedCount++;
        }

        String localizedCover = localizeSingleImageUrl(article.getCover(), context);
        if (!StringUtils.equals(localizedCover, article.getCover())) {
            update.setCover(localizedCover);
            CoverImageVo coverImage = CoverImageUtil.fromJson(null, localizedCover, article.getTitle());
            coverImage.setKey(localizedCover);
            coverImage.setFallback(localizedCover);
            update.setCoverImage(CoverImageUtil.toJson(coverImage));
            changedCount++;
        }

        if (changedCount <= 0) {
            return new LocalizeImagesResult(null, 0, warnings);
        }
        return new LocalizeImagesResult(update, changedCount, warnings);
    }

    private void renderChildren(String blockId, int depth, StringBuilder markdown, StringBuilder plainText, ImportContext context) {
        if (depth > MAX_BLOCK_DEPTH || context.getImportedBlocks() >= MAX_BLOCK_COUNT) {
            context.getWarnings().add("Notion 页面层级或块数量过大，已截断部分内容。");
            return;
        }

        String nextCursor = null;
        do {
            String path = "/blocks/" + urlEncode(blockId) + "/children?page_size=100";
            if (StringUtils.isNotBlank(nextCursor)) {
                path += "&start_cursor=" + urlEncode(nextCursor);
            }
            JSONObject response = requestNotionObjectWithRetry(path);
            JSONArray results = response.getJSONArray("results");
            if (results != null) {
                for (Object item : results) {
                    if (!(item instanceof JSONObject)) {
                        continue;
                    }
                    if (context.getImportedBlocks() >= MAX_BLOCK_COUNT) {
                        context.getWarnings().add("Notion 页面块数量超过 " + MAX_BLOCK_COUNT + "，已截断后续内容。");
                        return;
                    }
                    JSONObject block = (JSONObject) item;
                    context.setImportedBlocks(context.getImportedBlocks() + 1);
                    renderBlock(block, depth, markdown, plainText, context);
                }
            }
            nextCursor = Boolean.TRUE.equals(response.getBool("has_more", false)) ? response.getStr("next_cursor") : null;
        } while (StringUtils.isNotBlank(nextCursor));
    }

    private void renderBlock(JSONObject block, int depth, StringBuilder markdown, StringBuilder plainText, ImportContext context) {
        String type = block.getStr("type");
        JSONObject body = StringUtils.isBlank(type) ? null : block.getJSONObject(type);
        if (body == null) {
            return;
        }

        String indent = repeat("  ", Math.max(0, depth));
        switch (type) {
            case "paragraph":
                appendParagraph(markdown, plainText, body);
                break;
            case "heading_1":
                appendHeading(markdown, plainText, body, "# ");
                break;
            case "heading_2":
                appendHeading(markdown, plainText, body, "## ");
                break;
            case "heading_3":
                appendHeading(markdown, plainText, body, "### ");
                break;
            case "bulleted_list_item":
                appendListItem(markdown, plainText, body, indent + "- ");
                break;
            case "numbered_list_item":
                appendListItem(markdown, plainText, body, indent + "1. ");
                break;
            case "to_do":
                appendTodo(markdown, plainText, body, indent);
                break;
            case "quote":
                appendQuote(markdown, plainText, body);
                break;
            case "callout":
                appendCallout(markdown, plainText, body);
                break;
            case "code":
                appendCode(markdown, plainText, body);
                break;
            case "image":
                appendImage(markdown, plainText, body, context);
                break;
            case "divider":
                markdown.append("\n---\n\n");
                break;
            case "bookmark":
                appendBookmark(markdown, plainText, body);
                break;
            case "toggle":
                appendToggle(markdown, plainText, body);
                break;
            case "child_page":
                appendChildPage(markdown, plainText, body);
                break;
            case "child_database":
                appendChildDatabase(markdown, plainText, body);
                break;
            default:
                context.getWarnings().add("暂未支持 Notion 块类型：" + type);
                break;
        }

        if (Boolean.TRUE.equals(block.getBool("has_children", false))) {
            renderChildren(block.getStr("id"), depth + 1, markdown, plainText, context);
        }
    }

    private void appendParagraph(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append(text).append("\n\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendHeading(StringBuilder markdown, StringBuilder plainText, JSONObject body, String prefix) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append(prefix).append(text).append("\n\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendListItem(StringBuilder markdown, StringBuilder plainText, JSONObject body, String prefix) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append(prefix).append(text).append("\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendTodo(StringBuilder markdown, StringBuilder plainText, JSONObject body, String indent) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        String checked = Boolean.TRUE.equals(body.getBool("checked", false)) ? "x" : " ";
        markdown.append(indent).append("- [").append(checked).append("] ").append(text).append("\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendQuote(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append("> ").append(text.replace("\n", "\n> ")).append("\n\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendCallout(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append("> ").append(text.replace("\n", "\n> ")).append("\n\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendCode(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String code = richTextPlain(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(code)) {
            return;
        }
        String language = StringUtils.defaultIfBlank(body.getStr("language"), "");
        markdown.append("```").append(language).append("\n").append(code).append("\n```\n\n");
        appendPlainText(plainText, code);
    }

    private void appendImage(StringBuilder markdown, StringBuilder plainText, JSONObject body, ImportContext context) {
        String imageUrl = extractFileObjectUrl(body);
        if (StringUtils.isBlank(imageUrl)) {
            return;
        }
        String caption = richTextPlain(body.getJSONArray("caption"));
        String finalUrl = persistImageIfNeeded(imageUrl, context);
        markdown.append("![").append(escapeMarkdownText(caption)).append("](").append(finalUrl).append(")").append("\n\n");
        if (StringUtils.isNotBlank(caption)) {
            appendPlainText(plainText, caption);
        }
    }

    private void appendBookmark(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String url = body.getStr("url");
        if (StringUtils.isBlank(url)) {
            return;
        }
        markdown.append("[链接](").append(url).append(")").append("\n\n");
        appendPlainText(plainText, url);
    }

    private void appendToggle(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String text = renderRichText(body.getJSONArray("rich_text"));
        if (StringUtils.isBlank(text)) {
            return;
        }
        markdown.append("**").append(text).append("**\n\n");
        appendPlainText(plainText, richTextPlain(body.getJSONArray("rich_text")));
    }

    private void appendChildPage(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String title = body.getStr("title");
        if (StringUtils.isBlank(title)) {
            return;
        }
        markdown.append("## ").append(escapeMarkdownText(title)).append("\n\n");
        appendPlainText(plainText, title);
    }

    private void appendChildDatabase(StringBuilder markdown, StringBuilder plainText, JSONObject body) {
        String title = body.getStr("title");
        if (StringUtils.isBlank(title)) {
            title = "Notion 数据库";
        }
        markdown.append("> 已引用数据库：").append(escapeMarkdownText(title)).append("\n\n");
        appendPlainText(plainText, title);
    }

    private JSONObject requestNotionObjectWithRetry(String pathAndQuery) {
        ServiceException lastException = null;
        for (int attempt = 1; attempt <= NOTION_REQUEST_RETRY_TIMES; attempt++) {
            try {
                return requestNotionObjectByJdk(pathAndQuery);
            } catch (ServiceException ex) {
                if (isNotionHttpFailure(ex)) {
                    throw ex;
                }
                try {
                    return requestNotionObject(pathAndQuery);
                } catch (ServiceException fallbackEx) {
                    if (isNotionHttpFailure(fallbackEx)) {
                        throw fallbackEx;
                    }
                    fallbackEx.addSuppressed(ex);
                    lastException = fallbackEx;
                    if (attempt >= NOTION_REQUEST_RETRY_TIMES) {
                        break;
                    }
                    log.warn("Notion request failed, will retry. attempt={}/{}, path={}, primary={}, fallback={}",
                            attempt, NOTION_REQUEST_RETRY_TIMES, pathAndQuery, ex.getMessage(), fallbackEx.getMessage());
                    sleepBeforeRetry(attempt);
                }
            }
        }
        throw new ServiceException("Notion 页面读取失败，已重试 " + NOTION_REQUEST_RETRY_TIMES
                + " 次：" + (lastException == null ? "unknown" : lastException.getMessage()), lastException);
    }

    private boolean isNotionHttpFailure(ServiceException ex) {
        return ex != null && StringUtils.contains(ex.getMessage(), "HTTP ");
    }

    private void sleepBeforeRetry(int failedAttempt) {
        try {
            Thread.sleep(NOTION_REQUEST_RETRY_DELAY_MS * failedAttempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private JSONObject requestNotionObjectByJdk(String pathAndQuery) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(NOTION_API_BASE + pathAndQuery);
            connection = (HttpsURLConnection) url.openConnection(Proxy.NO_PROXY);
            connection.setConnectTimeout(NOTION_REQUEST_TIMEOUT_MS);
            connection.setReadTimeout(NOTION_REQUEST_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiToken.trim());
            connection.setRequestProperty("Notion-Version", StringUtils.defaultIfBlank(notionVersion, "2022-06-28"));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "boylu-blog/1.0");

            int status = connection.getResponseCode();
            String body = readResponseBody(connection, status);
            if (status < 200 || status >= 300) {
                throw new ServiceException("Notion 页面读取失败，请确认页面已共享给 Integration。HTTP "
                        + status + ": " + StrUtil.sub(body, 0, 180));
            }
            return JSONUtil.parseObj(body);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("Notion 页面读取失败：" + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpsURLConnection connection, int status) throws Exception {
        InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        try (InputStream input = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private JSONObject requestNotionObject(String pathAndQuery) {
        String url = NOTION_API_BASE + pathAndQuery;
        HttpResponse response = null;
        try {
            response = HttpRequest.get(url)
                    .timeout(NOTION_REQUEST_TIMEOUT_MS)
                    .header(Header.AUTHORIZATION, "Bearer " + apiToken.trim())
                    .header("Notion-Version", StringUtils.defaultIfBlank(notionVersion, "2022-06-28"))
                    .header(Header.CONTENT_TYPE, "application/json")
                    .execute();
            if (!response.isOk()) {
                String body = StringUtils.defaultString(response.body());
                throw new ServiceException("Notion 页面读取失败，请确认页面已共享给 Integration。HTTP "
                        + response.getStatus() + ": " + StrUtil.sub(body, 0, 180));
            }
            return JSONUtil.parseObj(response.body());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("Notion 页面读取失败：" + ex.getMessage(), ex);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String persistImageIfNeeded(String imageUrl, ImportContext context) {
        if (!context.isImportImages()) {
            return imageUrl;
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        String cachedUrl = context.getImageUrlCache().get(imageUrl);
        if (StringUtils.isNotBlank(cachedUrl)) {
            return cachedUrl;
        }

        HttpResponse response = null;
        Path tempPath = null;
        try {
            response = HttpRequest.get(imageUrl).timeout(30000).execute();
            if (!response.isOk()) {
                context.getWarnings().add("图片下载失败，已保留原链接：" + shortText(imageUrl));
                return imageUrl;
            }

            byte[] bytes = response.bodyBytes();
            long maxBytes = Math.max(1L, maxImageSizeMb) * 1024L * 1024L;
            if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
                context.getWarnings().add("图片过大或为空，已保留原链接：" + shortText(imageUrl));
                return imageUrl;
            }

            String contentType = StringUtils.defaultIfBlank(response.header(Header.CONTENT_TYPE), "image/jpeg");
            if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                context.getWarnings().add("Notion 图片响应不是图片类型，已保留原链接：" + shortText(imageUrl));
                return imageUrl;
            }

            String extension = resolveImageExtension(contentType, imageUrl);
            String saveFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            tempPath = Files.createTempFile("notion-img-", "." + extension);
            Files.write(tempPath, bytes);

            String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/notion/";
            FileInfo fileInfo = fileStorageService.of(tempPath.toFile(), saveFilename, contentType, (long) bytes.length)
                    .setPath(path)
                    .setSaveFilename(saveFilename)
                    .setContentType(contentType)
                    .setOriginalFilename(resolveOriginalFilename(imageUrl, saveFilename))
                    .putAttr("source", "notion-import")
                    .upload();
            String publicUrl = buildPublicContentUrl(fileInfo);
            String resultUrl = StringUtils.defaultIfBlank(publicUrl, imageUrl);
            context.getImageUrlCache().put(imageUrl, resultUrl);
            return resultUrl;
        } catch (Exception ex) {
            log.warn("Failed to persist Notion image, url={}", imageUrl, ex);
            context.getWarnings().add("图片保存到本站失败，已保留原链接：" + shortText(imageUrl));
            return imageUrl;
        } finally {
            if (response != null) {
                response.close();
            }
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (Exception ex) {
                    log.warn("Failed to delete Notion image temp file, path={}", tempPath, ex);
                }
            }
        }
    }

    private String replaceMarkdownImages(String markdown, ImportContext context) {
        if (StringUtils.isBlank(markdown)) {
            return markdown;
        }
        Matcher matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(markdown);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            String localizedUrl = localizeSingleImageUrl(originalUrl, context);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0).replace(originalUrl, localizedUrl)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceHtmlImages(String html, ImportContext context) {
        if (StringUtils.isBlank(html)) {
            return html;
        }
        Matcher matcher = HTML_IMAGE_SRC_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String originalUrl = matcher.group(2);
            String localizedUrl = localizeSingleImageUrl(originalUrl, context);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + localizedUrl + matcher.group(3)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String localizeSingleImageUrl(String imageUrl, ImportContext context) {
        if (StringUtils.isBlank(imageUrl) || !isRemoteImageUrl(imageUrl)) {
            return imageUrl;
        }
        String downloadUrl = imageUrl.replace("&amp;", "&");
        return persistImageIfNeeded(downloadUrl, context);
    }

    private boolean isRemoteImageUrl(String imageUrl) {
        String lower = StringUtils.defaultString(imageUrl).toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && !lower.contains("boylu.cn/")
                && !lower.contains("www.boylu.cn/")
                && !lower.contains("boylu.top/");
    }

    private String buildPublicContentUrl(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        String fileId = fileInfo.getId();
        if (StringUtils.isBlank(fileId) && StringUtils.isNotBlank(fileInfo.getUrl())) {
            FileDetail fileDetail = fileDetailService.getOne(new LambdaQueryWrapper<FileDetail>()
                    .eq(FileDetail::getUrl, fileInfo.getUrl())
                    .last("limit 1"));
            if (fileDetail != null) {
                fileId = fileDetail.getId();
            }
        }
        if (StringUtils.isNotBlank(fileId)) {
            return getPublicFileContentPrefix() + fileId;
        }
        return fileInfo.getUrl();
    }

    private String resolveCover(NotionImportDto dto, JSONObject page, ImportContext context) {
        if (StringUtils.isNotBlank(dto.getCover())) {
            return dto.getCover().trim();
        }
        JSONObject cover = page.getJSONObject("cover");
        String coverUrl = extractFileObjectUrl(cover);
        if (StringUtils.isBlank(coverUrl)) {
            return "";
        }
        return persistImageIfNeeded(coverUrl, context);
    }

    private String extractFileObjectUrl(JSONObject object) {
        if (object == null) {
            return null;
        }
        String type = object.getStr("type");
        if ("external".equals(type)) {
            JSONObject external = object.getJSONObject("external");
            return external == null ? null : external.getStr("url");
        }
        if ("file".equals(type)) {
            JSONObject file = object.getJSONObject("file");
            return file == null ? null : file.getStr("url");
        }
        return null;
    }

    private String extractPageTitle(JSONObject page) {
        JSONObject properties = page.getJSONObject("properties");
        if (properties == null) {
            return null;
        }
        for (String key : properties.keySet()) {
            JSONObject property = properties.getJSONObject(key);
            if (property != null && "title".equals(property.getStr("type"))) {
                return richTextPlain(property.getJSONArray("title")).trim();
            }
        }
        return null;
    }

    private String renderRichText(JSONArray richTextArray) {
        if (richTextArray == null || richTextArray.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object item : richTextArray) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject richText = (JSONObject) item;
            String text = escapeMarkdownText(StringUtils.defaultString(richText.getStr("plain_text")));
            JSONObject annotations = richText.getJSONObject("annotations");
            String href = richText.getStr("href");

            if (annotations != null && Boolean.TRUE.equals(annotations.getBool("code", false))) {
                text = "`" + text.replace("`", "\\`") + "`";
            }
            if (annotations != null && Boolean.TRUE.equals(annotations.getBool("bold", false))) {
                text = "**" + text + "**";
            }
            if (annotations != null && Boolean.TRUE.equals(annotations.getBool("italic", false))) {
                text = "*" + text + "*";
            }
            if (annotations != null && Boolean.TRUE.equals(annotations.getBool("strikethrough", false))) {
                text = "~~" + text + "~~";
            }
            if (annotations != null && Boolean.TRUE.equals(annotations.getBool("underline", false))) {
                text = "<u>" + text + "</u>";
            }
            if (StringUtils.isNotBlank(href)) {
                text = "[" + text + "](" + href + ")";
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private String richTextPlain(JSONArray richTextArray) {
        if (richTextArray == null || richTextArray.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object item : richTextArray) {
            if (item instanceof JSONObject) {
                builder.append(StringUtils.defaultString(((JSONObject) item).getStr("plain_text")));
            }
        }
        return builder.toString();
    }

    private String renderMarkdown(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(parser.parse(StringUtils.defaultString(markdown)));
    }

    private List<String> normalizeTags(List<String> tags) {
        List<String> result = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                if (StringUtils.isNotBlank(tag)) {
                    String normalized = tag.trim();
                    if (!result.contains(normalized)) {
                        result.add(normalized);
                    }
                }
            }
        }
        if (result.isEmpty()) {
            result.add("Notion");
        }
        return result;
    }

    private String buildSummary(String text) {
        String normalized = StringUtils.defaultString(text).replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180);
    }

    private boolean resolveImportImages(NotionImportDto dto) {
        return dto.getImportImages() == null ? defaultImportImages : dto.getImportImages();
    }

    private String extractPageId(String input) {
        String normalized = StringUtils.defaultString(input).trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int hashIndex = normalized.indexOf('#');
        if (hashIndex >= 0) {
            normalized = normalized.substring(0, hashIndex);
        }

        Matcher uuidMatcher = UUID_PATTERN.matcher(normalized);
        if (uuidMatcher.find()) {
            return uuidMatcher.group(1);
        }
        Matcher compactMatcher = COMPACT_UUID_PATTERN.matcher(normalized.replace("-", ""));
        if (compactMatcher.find()) {
            String value = compactMatcher.group(1);
            return value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16)
                    + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }
        throw new ServiceException("Notion 页面地址或 Page ID 格式不正确");
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String getPublicFileContentPrefix() {
        String prefix = StringUtils.defaultIfBlank(publicFileContentPrefix, DEFAULT_PUBLIC_FILE_CONTENT_PREFIX).trim();
        if (!prefix.startsWith("http://") && !prefix.startsWith("https://") && !prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    private String resolveImageExtension(String contentType, String imageUrl) {
        String type = StringUtils.defaultString(contentType).toLowerCase(Locale.ROOT);
        if (type.contains("png")) {
            return "png";
        }
        if (type.contains("webp")) {
            return "webp";
        }
        if (type.contains("gif")) {
            return "gif";
        }
        if (type.contains("svg")) {
            return "svg";
        }
        if (type.contains("jpeg") || type.contains("jpg")) {
            return "jpg";
        }
        String path = StringUtils.defaultString(imageUrl).toLowerCase(Locale.ROOT);
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = path.substring(dotIndex + 1).replaceAll("[^a-z0-9].*$", "");
            if (ext.matches("png|webp|gif|svg|jpg|jpeg")) {
                return "jpeg".equals(ext) ? "jpg" : ext;
            }
        }
        return "jpg";
    }

    private String resolveOriginalFilename(String imageUrl, String fallback) {
        try {
            String path = new URI(imageUrl).getPath();
            if (StringUtils.isNotBlank(path)) {
                String filename = path.substring(path.lastIndexOf('/') + 1);
                if (StringUtils.isNotBlank(filename)) {
                    return filename;
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String escapeMarkdownText(String text) {
        return StringUtils.defaultString(text)
                .replace("\\", "\\\\")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    private void appendPlainText(StringBuilder plainText, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        if (plainText.length() > 0) {
            plainText.append(' ');
        }
        plainText.append(text.trim());
    }

    private String repeat(String value, int count) {
        if (count <= 0) {
            return "";
        }
        return String.join("", Collections.nCopies(count, value));
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception ex) {
            throw new ServiceException("URL 编码失败");
        }
    }

    private String shortText(String value) {
        return StrUtil.sub(StringUtils.defaultString(value), 0, 90);
    }

    @Data
    private static class ImportContext {
        private final boolean importImages;
        private final List<String> warnings;
        private final Map<String, String> imageUrlCache = new HashMap<>();
        private int importedBlocks;
    }

    @Data
    @AllArgsConstructor
    public static class ImportPageResult {
        private SysArticleDetailVo article;
        private Integer importedBlocks;
        private List<String> warnings;
    }

    @Data
    @AllArgsConstructor
    public static class LocalizeImagesResult {
        private SysArticle article;
        private Integer changedCount;
        private List<String> warnings;
    }
}
