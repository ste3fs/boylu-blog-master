package com.boylu.service;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.entity.FileDetail;
import com.boylu.exception.ServiceException;
import com.boylu.utils.CoverImageUtil;
import com.boylu.utils.DateUtil;
import com.boylu.vo.article.CoverImageVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleCoverImageService {

    private static final int[] RESPONSIVE_WIDTHS = new int[]{320, 640, 960, 1280, 1600};
    private static final int[] JPG_WIDTHS = new int[]{640, 960, 1280};
    private static final String PUBLIC_FILE_CONTENT_PREFIX = "/boylu/file/content/";

    private final FileStorageService fileStorageService;
    private final FileDetailService fileDetailService;

    public CoverImageVo process(MultipartFile file, String path, String source) {
        return process(readAllBytes(file), file.getOriginalFilename(), file.getContentType(), path, source);
    }

    public CoverImageVo process(byte[] originalBytes, String originalFilename, String contentType, String path, String source) {
        BufferedImage originalImage = readImage(originalBytes);
        String hash = shortSha256(originalBytes);
        String originalExtension = resolveOriginalExtension(originalFilename, contentType);
        String normalizedPath = StringUtils.defaultIfBlank(path, DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/article-cover/");

        uploadBytes(
                originalBytes,
                normalizedPath + "original/",
                "cover-" + hash + "-original." + originalExtension,
                StringUtils.defaultIfBlank(contentType, "image/" + originalExtension),
                source
        );

        return buildCoverImage(originalImage, hash, normalizedPath, source);
    }

    public CoverImageVo process(Path originalPath, String originalFilename, String contentType, String path, String source) {
        BufferedImage originalImage = readImage(originalPath);
        String hash = shortSha256(originalPath);
        String originalExtension = resolveOriginalExtension(originalFilename, contentType);
        String normalizedPath = StringUtils.defaultIfBlank(path, DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/article-cover/");

        uploadFile(
                originalPath,
                normalizedPath + "original/",
                "cover-" + hash + "-original." + originalExtension,
                StringUtils.defaultIfBlank(contentType, "image/" + originalExtension),
                source
        );

        return buildCoverImage(originalImage, hash, normalizedPath, source);
    }

    private CoverImageVo buildCoverImage(BufferedImage originalImage, String hash, String normalizedPath, String source) {
        String key = StringUtils.defaultString(source, "article-cover") + "/" + hash;
        Map<String, Map<Integer, String>> variants = new LinkedHashMap<String, Map<Integer, String>>();
        Map<Integer, String> avifVariants = buildVariants(originalImage, normalizedPath, "avif", "image/avif", RESPONSIVE_WIDTHS, 0.58f, hash, source);
        Map<Integer, String> webpVariants = buildVariants(originalImage, normalizedPath, "webp", "image/webp", RESPONSIVE_WIDTHS, 0.76f, hash, source);
        Map<Integer, String> jpgVariants = buildVariants(originalImage, normalizedPath, "jpg", "image/jpeg", JPG_WIDTHS, 0.82f, hash, source);

        if (!avifVariants.isEmpty()) {
            variants.put("avif", avifVariants);
        }
        if (!webpVariants.isEmpty()) {
            variants.put("webp", webpVariants);
        }
        variants.put("jpg", jpgVariants);

        String fallback = chooseFallback(jpgVariants, webpVariants);

        return CoverImageVo.builder()
                .key(key)
                .alt("")
                .width(originalImage.getWidth())
                .height(originalImage.getHeight())
                .dominantColor(resolveDominantColor(originalImage))
                .blurDataURL(buildBlurDataUrl(originalImage))
                .variants(variants)
                .fallback(StringUtils.defaultIfBlank(fallback, CoverImageUtil.FALLBACK_IMAGE))
                .hash(hash)
                .build();
    }

    private Map<Integer, String> buildVariants(BufferedImage originalImage, String path, String format,
                                               String contentType, int[] widths, float quality,
                                               String hash, String source) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();
        if (!hasImageWriter(format)) {
            return result;
        }

        for (int width : widths) {
            try {
                BufferedImage resized = resizeToWidth(originalImage, width);
                byte[] bytes = encodeImage(resized, format, quality);
                if (bytes == null || bytes.length == 0) {
                    continue;
                }

                String filename = "cover-" + hash + "-" + width + "." + format;
                FileInfo fileInfo = uploadBytes(bytes, path, filename, contentType, source);
                String url = buildPublicContentUrl(fileInfo);
                if (StringUtils.isNotBlank(url)) {
                    result.put(width, url);
                }
            } catch (Exception ex) {
                log.warn("Failed to generate article cover variant, format={}, width={}", format, width, ex);
            }
        }
        return result;
    }

    private FileInfo uploadBytes(byte[] bytes, String path, String filename, String contentType, String source) {
        BytesMultipartFile multipartFile = new BytesMultipartFile("file", filename, contentType, bytes);
        FileInfo fileInfo = fileStorageService.of(multipartFile)
                .setPath(path)
                .setSaveFilename(filename)
                .setContentType(contentType)
                .setOriginalFilename(filename)
                .setAttr(Dict.create().set("source", source).set("role", "article-cover"))
                .upload();
        if (fileInfo == null) {
            throw new ServiceException("文章封面图片处理失败");
        }
        return fileInfo;
    }

    private FileInfo uploadFile(Path sourcePath, String path, String filename, String contentType, String source) {
        try {
            FileInfo fileInfo = fileStorageService.of(sourcePath.toFile(), filename, contentType, Files.size(sourcePath))
                    .setPath(path)
                    .setSaveFilename(filename)
                    .setContentType(contentType)
                    .setOriginalFilename(filename)
                    .setAttr(Dict.create().set("source", source).set("role", "article-cover"))
                    .upload();
            if (fileInfo == null) {
                throw new ServiceException("文章封面图片处理失败");
            }
            return fileInfo;
        } catch (IOException ex) {
            throw new ServiceException("读取文章封面图片失败");
        }
    }

    private BufferedImage resizeToWidth(BufferedImage originalImage, int targetWidth) {
        int targetHeight = Math.max(1, Math.round((float) originalImage.getHeight() * targetWidth / originalImage.getWidth()));
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
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

    private boolean hasImageWriter(String format) {
        return ImageIO.getImageWritersByFormatName(format).hasNext();
    }

    private String resolveDominantColor(BufferedImage image) {
        BufferedImage scaled = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.drawImage(image, 0, 0, 1, 1, null);
        graphics.dispose();
        int rgb = scaled.getRGB(0, 0);
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    private String buildBlurDataUrl(BufferedImage image) {
        try {
            BufferedImage blur = resizeToWidth(image, 24);
            byte[] bytes = encodeImage(blur, "jpg", 0.45f);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private String chooseFallback(Map<Integer, String> jpgVariants, Map<Integer, String> webpVariants) {
        if (jpgVariants.containsKey(960)) {
            return jpgVariants.get(960);
        }
        if (jpgVariants.containsKey(640)) {
            return jpgVariants.get(640);
        }
        if (webpVariants.containsKey(960)) {
            return webpVariants.get(960);
        }
        if (webpVariants.containsKey(640)) {
            return webpVariants.get(640);
        }
        return null;
    }

    private byte[] readAllBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ServiceException("读取上传图片失败");
        }
    }

    private BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ServiceException("无法解析上传图片");
            }
            return image;
        } catch (IOException ex) {
            throw new ServiceException("无法解析上传图片");
        }
    }

    private BufferedImage readImage(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ServiceException("无法解析上传图片");
            }
            return image;
        } catch (IOException ex) {
            throw new ServiceException("无法解析上传图片");
        }
    }

    private String resolveOriginalExtension(MultipartFile file) {
        return resolveOriginalExtension(file.getOriginalFilename(), file.getContentType());
    }

    private String resolveOriginalExtension(String filename, String contentType) {
        if (StringUtils.isNotBlank(filename) && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if ("jpeg".equals(ext)) {
                return "jpg";
            }
            if (ext.matches("jpg|png|webp|gif")) {
                return ext;
            }
        }
        String normalizedContentType = StringUtils.defaultString(contentType).toLowerCase(Locale.ROOT);
        if (normalizedContentType.contains("png")) {
            return "png";
        }
        if (normalizedContentType.contains("webp")) {
            return "webp";
        }
        if (normalizedContentType.contains("gif")) {
            return "gif";
        }
        return "jpg";
    }

    private String shortSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            return String.valueOf(Math.abs(new String(bytes, StandardCharsets.ISO_8859_1).hashCode()));
        }
    }

    private String shortSha256(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            try {
                return String.valueOf(Math.abs((path.toString() + ":" + Files.size(path)).hashCode()));
            } catch (IOException ignored) {
                return String.valueOf(Math.abs(path.toString().hashCode()));
            }
        }
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
            return PUBLIC_FILE_CONTENT_PREFIX + fileId;
        }

        return fileInfo.getUrl();
    }

    private static class BytesMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private BytesMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
