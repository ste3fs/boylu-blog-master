package com.boylu.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.entity.FileDetail;
import com.boylu.entity.SysFileOss;
import com.boylu.service.FileDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/img")
@RequiredArgsConstructor
@Slf4j
public class LocalImageStyleController {

    private static final String LOCAL_FILE_PREFIX = "/localFile/";
    private static final String CACHE_PREFIX = "img-cache/";
    private static final List<Integer> ALLOWED_WIDTHS = Arrays.asList(160, 240, 320, 480, 640, 960, 1280);
    private static final Pattern STYLE_PATTERN = Pattern.compile("^([A-Za-z0-9_-]+)!w(\\d{2,4})\\.(webp|jpg|jpeg)$", Pattern.CASE_INSENSITIVE);
    private static final float WEBP_QUALITY = 0.72f;
    private static final float JPG_QUALITY = 0.78f;
    private static final long MB = 1024L * 1024L;
    private static final long MAX_SOURCE_SIZE_BYTES = 30L * MB;
    private static final long MAX_SOURCE_PIXELS = 36_000_000L;
    private static final Map<String, Object> CACHE_LOCKS = new ConcurrentHashMap<String, Object>();

    private final FileDetailService fileDetailService;

    @GetMapping("/local/{style:.+}")
    public void local(@PathVariable String style, HttpServletResponse response) throws IOException {
        ImageRequest request = parseStyle(style);
        if (request == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String sourceUrl = normalizeSourceUrl(decodeBase64Url(request.encodedSource));
        if (StringUtils.isBlank(sourceUrl)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Path sourcePath = resolveSourcePath(sourceUrl);
        if (sourcePath == null || !Files.isRegularFile(sourcePath)) {
            String legacyTarget = resolveLegacyTarget(sourceUrl);
            if (StringUtils.isNotBlank(legacyTarget)) {
                redirect(legacyTarget, response);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ImageInfo imageInfo = readImageInfo(sourcePath);
        if (imageInfo == null || imageInfo.shouldBypassStyle(request.format)) {
            redirect(sourceUrl, response);
            return;
        }

        try {
            Path cachePath = buildCacheImage(sourcePath, request.width, request.format);
            String targetUrl = LOCAL_FILE_PREFIX + getLocalStorageRootPath(null).relativize(cachePath).toString().replace("\\", "/");
            redirect(targetUrl, response);
        } catch (Exception ex) {
            log.warn("Failed to build local styled image, source={}, style={}", sourceUrl, style, ex);
            redirect(sourceUrl, response);
        }
    }

    private ImageRequest parseStyle(String style) {
        if (StringUtils.isBlank(style)) {
            return null;
        }
        Matcher matcher = STYLE_PATTERN.matcher(style.trim());
        if (!matcher.matches()) {
            return null;
        }
        int width = Integer.parseInt(matcher.group(2));
        if (!ALLOWED_WIDTHS.contains(width)) {
            return null;
        }
        String format = matcher.group(3).toLowerCase(Locale.ROOT);
        if ("jpeg".equals(format)) {
            format = "jpg";
        }
        return new ImageRequest(matcher.group(1), width, format);
    }

    private String decodeBase64Url(String encoded) {
        try {
            int padding = (4 - encoded.length() % 4) % 4;
            String value = encoded + "====".substring(0, padding);
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeSourceUrl(String rawUrl) {
        if (StringUtils.isBlank(rawUrl) || rawUrl.contains("\n") || rawUrl.contains("\r")) {
            return null;
        }
        String url = rawUrl.trim();
        try {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
        }
        if (url.startsWith("//")) {
            url = url.replaceFirst("^//[^/]+", "");
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                URL parsed = new URL(url);
                url = parsed.getPath() + (StringUtils.defaultString(parsed.getQuery()).isEmpty() ? "" : "?" + parsed.getQuery());
            } catch (Exception ignored) {
                return null;
            }
        }

        if (url.startsWith(LOCAL_FILE_PREFIX)) {
            return url;
        }
        if (StringUtils.isNotBlank(extractPublicFileId(url))) {
            return url;
        }
        return null;
    }

    private Path resolveSourcePath(String sourceUrl) {
        String fileId = extractPublicFileId(sourceUrl);
        if (StringUtils.isNotBlank(fileId)) {
            FileDetail fileDetail = fileDetailService.getById(fileId);
            return resolveLocalFilePath(fileDetail);
        }

        if (sourceUrl.startsWith(LOCAL_FILE_PREFIX)) {
            return resolveLocalFilePathFromUrl(sourceUrl);
        }
        return null;
    }

    private String resolveLegacyTarget(String sourceUrl) {
        String fileId = extractPublicFileId(sourceUrl);
        if (StringUtils.isBlank(fileId)) {
            return sourceUrl.startsWith(LOCAL_FILE_PREFIX) ? sourceUrl : null;
        }
        FileDetail fileDetail = fileDetailService.getById(fileId);
        if (fileDetail == null) {
            return null;
        }
        String target = normalizeStoredUrl(fileDetail.getUrl());
        if (StringUtils.isNotBlank(target)) {
            return target;
        }
        return normalizeStoredUrl(fileDetail.getThUrl());
    }

    private ImageInfo readImageInfo(Path sourcePath) {
        try {
            if (Files.size(sourcePath) > MAX_SOURCE_SIZE_BYTES) {
                return ImageInfo.bypass();
            }
            try (ImageInputStream inputStream = ImageIO.createImageInputStream(sourcePath.toFile())) {
                if (inputStream == null) {
                    return null;
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
                if (!readers.hasNext()) {
                    return null;
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(inputStream, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    String format = StringUtils.defaultString(reader.getFormatName()).toLowerCase(Locale.ROOT);
                    if ((long) width * (long) height > MAX_SOURCE_PIXELS) {
                        return ImageInfo.bypass();
                    }
                    return new ImageInfo(format, width, height, false);
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private Path buildCacheImage(Path sourcePath, int width, String format) throws Exception {
        String sourceHash = sha256(sourcePath);
        Path rootPath = getLocalStorageRootPath(null);
        Path cachePath = rootPath
                .resolve(CACHE_PREFIX)
                .resolve(sourceHash.substring(0, 2))
                .resolve(sourceHash.substring(2, 4))
                .resolve(sourceHash + "-w" + width + "." + format)
                .normalize();
        if (!cachePath.startsWith(rootPath)) {
            throw new IOException("unsafe cache path");
        }
        if (Files.isRegularFile(cachePath)) {
            return cachePath;
        }

        Object lock = CACHE_LOCKS.computeIfAbsent(cachePath.toString(), key -> new Object());
        synchronized (lock) {
            try {
                if (Files.isRegularFile(cachePath)) {
                    return cachePath;
                }

                BufferedImage original = ImageIO.read(sourcePath.toFile());
                if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
                    throw new IOException("unsupported image");
                }

                if (original.getColorModel().hasAlpha() && "jpg".equals(format)) {
                    throw new IOException("transparent image should not be flattened to jpg");
                }

                int targetWidth = Math.min(width, original.getWidth());
                BufferedImage resized = resizeToWidth(original, targetWidth, "webp".equals(format));
                byte[] output = encodeImage(resized, format, "webp".equals(format) ? WEBP_QUALITY : JPG_QUALITY);
                if (output == null || output.length == 0) {
                    throw new IOException("image writer missing");
                }

                Files.createDirectories(cachePath.getParent());
                Path tempPath = cachePath.resolveSibling(cachePath.getFileName().toString() + "." + UUID.randomUUID() + ".tmp");
                Files.write(tempPath, output);
                try {
                    Files.move(tempPath, cachePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    if (!Files.isRegularFile(cachePath)) {
                        throw ex;
                    }
                    Files.deleteIfExists(tempPath);
                }
                return cachePath;
            } finally {
                CACHE_LOCKS.remove(cachePath.toString(), lock);
            }
        }
    }

    private BufferedImage resizeToWidth(BufferedImage original, int targetWidth, boolean preserveAlpha) {
        int targetHeight = Math.max(1, Math.round((float) original.getHeight() * targetWidth / original.getWidth()));
        boolean useAlpha = preserveAlpha && original.getColorModel().hasAlpha();
        BufferedImage output = new BufferedImage(
                targetWidth,
                targetHeight,
                useAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!useAlpha) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
        }
        graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return output;
    }

    private byte[] encodeImage(BufferedImage image, String format, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = param.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0 && param.getCompressionType() == null) {
                    param.setCompressionType(compressionTypes[0]);
                }
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return outputStream.toByteArray();
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String extractPublicFileId(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        String normalized = url.trim();
        String[] prefixes = {
                "/boylu/file/content/",
                "/boylu/file/view/",
                "/mojian/file/content/",
                "/mojian/file/view/",
                "/file/content/",
                "/file/view/"
        };
        for (String prefix : prefixes) {
            int index = normalized.indexOf(prefix);
            if (index < 0) {
                continue;
            }
            String id = normalized.substring(index + prefix.length());
            int end = id.length();
            int queryIndex = id.indexOf('?');
            if (queryIndex >= 0) {
                end = Math.min(end, queryIndex);
            }
            int hashIndex = id.indexOf('#');
            if (hashIndex >= 0) {
                end = Math.min(end, hashIndex);
            }
            return id.substring(0, end);
        }
        return null;
    }

    private Path resolveLocalFilePath(FileDetail fileDetail) {
        if (fileDetail == null) {
            return null;
        }
        Path rootPath = getLocalStorageRootPath(fileDetail);
        if (rootPath == null) {
            return null;
        }
        Path filePath = buildSafeLocalPath(rootPath, joinRelativePath(fileDetail.getBasePath(), fileDetail.getPath(), fileDetail.getFilename()));
        if (filePath != null && Files.isRegularFile(filePath)) {
            return filePath;
        }
        String normalizedUrl = normalizeStoredUrl(fileDetail.getUrl());
        if (StringUtils.isNotBlank(normalizedUrl) && normalizedUrl.startsWith(LOCAL_FILE_PREFIX)) {
            return resolveLocalFilePathFromUrl(normalizedUrl);
        }
        return null;
    }

    private Path resolveLocalFilePathFromUrl(String url) {
        Path rootPath = getLocalStorageRootPath(null);
        if (rootPath == null || StringUtils.isBlank(url) || !url.startsWith(LOCAL_FILE_PREFIX)) {
            return null;
        }
        return buildSafeLocalPath(rootPath, url.substring(LOCAL_FILE_PREFIX.length()));
    }

    private Path getLocalStorageRootPath(FileDetail fileDetail) {
        String storagePath = null;
        List<SysFileOss> ossConfigs = fileDetailService.getOssConfig();
        if (ossConfigs != null) {
            for (SysFileOss config : ossConfigs) {
                if (config == null || !"local".equalsIgnoreCase(config.getPlatform())) {
                    continue;
                }
                if (fileDetail == null
                        || StringUtils.isBlank(fileDetail.getBasePath())
                        || StringUtils.equals(trimSlashes(config.getBasePath()), trimSlashes(fileDetail.getBasePath()))) {
                    storagePath = config.getStoragePath();
                    break;
                }
                if (StringUtils.isBlank(storagePath)) {
                    storagePath = config.getStoragePath();
                }
            }
        }
        if (StringUtils.isBlank(storagePath)) {
            storagePath = "storage";
        }
        return Paths.get(storagePath).toAbsolutePath().normalize();
    }

    private Path buildSafeLocalPath(Path rootPath, String relativePath) {
        if (rootPath == null || StringUtils.isBlank(relativePath)) {
            return null;
        }
        Path filePath = rootPath.resolve(trimLeadingSlash(relativePath).replace("\\", "/")).normalize();
        if (!filePath.startsWith(rootPath)) {
            return null;
        }
        return filePath;
    }

    private String joinRelativePath(String basePath, String path, String filename) {
        StringBuilder builder = new StringBuilder();
        appendPathPart(builder, basePath);
        appendPathPart(builder, path);
        if (StringUtils.isNotBlank(filename) && !builder.toString().endsWith(filename)) {
            appendPathPart(builder, filename);
        }
        return builder.toString();
    }

    private void appendPathPart(StringBuilder builder, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        String normalized = trimLeadingSlash(value).replace("\\", "/");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (StringUtils.isBlank(normalized)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("/");
        }
        builder.append(normalized);
    }

    private String normalizeStoredUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (url.contains(LOCAL_FILE_PREFIX)) {
            return url.substring(url.indexOf(LOCAL_FILE_PREFIX));
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith(LOCAL_FILE_PREFIX)) {
            return url;
        }
        return null;
    }

    private String trimLeadingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String trimSlashes(String value) {
        String normalized = trimLeadingSlash(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void redirect(String targetUrl, HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", targetUrl);
    }

    private static class ImageRequest {
        private final String encodedSource;
        private final int width;
        private final String format;

        private ImageRequest(String encodedSource, int width, String format) {
            this.encodedSource = encodedSource;
            this.width = width;
            this.format = format;
        }
    }

    private static class ImageInfo {
        private final String format;
        private final int width;
        private final int height;
        private final boolean forceBypass;

        private ImageInfo(String format, int width, int height, boolean forceBypass) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.forceBypass = forceBypass;
        }

        private static ImageInfo bypass() {
            return new ImageInfo("", 0, 0, true);
        }

        private boolean shouldBypassStyle(String targetFormat) {
            if (forceBypass) {
                return true;
            }
            if ("gif".equals(format)) {
                return true;
            }
            return width <= 0 || height <= 0 || StringUtils.isBlank(targetFormat);
        }
    }
}
