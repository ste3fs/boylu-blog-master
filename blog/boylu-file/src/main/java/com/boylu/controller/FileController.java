package com.boylu.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.common.Constants;
import com.boylu.common.RedisConstants;
import com.boylu.common.Result;
import com.boylu.entity.FileDetail;
import com.boylu.entity.SysFileOss;
import com.boylu.exception.ServiceException;
import com.boylu.service.FileDetailService;
import com.boylu.utils.DateUtil;
import com.boylu.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
@RequiredArgsConstructor
public class FileController {

    private static final String DEFAULT_PUBLIC_FILE_CONTENT_PREFIX = "/boylu/file/content/";
    private static final long MB = 1024L * 1024L;
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");

    private final FileDetailService fileDetailService;

    private final FileStorageService fileStorageService;

    private final RedisUtil redisUtil;

    @Value("${app.file.public-prefix:" + DEFAULT_PUBLIC_FILE_CONTENT_PREFIX + "}")
    private String publicFileContentPrefix;

    @Value("${app.file.max-upload-size-mb:30}")
    private long maxUploadSizeMb;

    @SaCheckLogin
    @GetMapping("/list")
    @ApiOperation(value = "获取文件记录列表")
    public Result<IPage<FileDetail>> list(FileDetail fileDetail) {
        return Result.success(fileDetailService.selectPage(fileDetail));
    }

    @SaCheckLogin
    @GetMapping("/getOssConfig")
    @ApiOperation(value = "获取存储平台配置")
    public Result<List<SysFileOss>> getOssConfig() {
        return Result.success(fileDetailService.getOssConfig());
    }

    @SaCheckLogin
    @PostMapping("/addOss")
    @SaCheckPermission("sys:oss:submit")
    @ApiOperation(value = "添加存储平台配置")
    public Result<Void> addOss(@RequestBody SysFileOss sysFileOss) {
        fileDetailService.addOss(sysFileOss);
        if (sysFileOss.getIsEnable() == Constants.YES) {
            fileStorageService.getProperties().setDefaultPlatform(sysFileOss.getPlatform());
        }
        return Result.success();
    }

    @SaCheckLogin
    @PutMapping("/updateOss")
    @SaCheckPermission("sys:oss:submit")
    @ApiOperation(value = "修改存储平台配置")
    public Result<Void> updateOss(@RequestBody SysFileOss sysFileOss) {
        fileDetailService.updateOss(sysFileOss);
        if (sysFileOss.getIsEnable() == Constants.YES) {
            fileStorageService.getProperties().setDefaultPlatform(sysFileOss.getPlatform());
        }
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/upload")
    @ApiOperation(value = "上传文件")
    public Result<String> upload(MultipartFile file, String source) {
        validateUploadFile(file);
        String normalizedSource = normalizeSource(source);
        DetectedImageType imageType = detectImageType(file);

        String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/";
        if (StringUtils.isNotBlank(normalizedSource)) {
            path = path + normalizedSource + "/";
        }

        String saveFilename = UUID.randomUUID().toString().replace("-", "") + "." + imageType.getExtension();

        FileInfo fileInfo = fileStorageService.of(file)
                .setPath(path)
                .setSaveFilename(saveFilename)
                .putAttr("source", normalizedSource)
                .upload();

        if (fileInfo == null) {
            throw new ServiceException("上传文件失败");
        }
        return Result.success(buildPublicContentUrl(fileInfo));
    }

    @GetMapping("/content/{id}")
    @ApiOperation(value = "根据文件 ID 访问文件")
    public void content(@PathVariable String id, HttpServletResponse response) throws IOException {
        redirectToFile(id, response);
    }

    @GetMapping("/view/{id}")
    @ApiOperation(value = "根据文件 ID 预览文件")
    public void view(@PathVariable String id, HttpServletResponse response) throws IOException {
        redirectToFile(id, response);
    }

    @GetMapping("/delete")
    @ApiOperation(value = "删除文件")
    @SaCheckPermission("sys:file:delete")
    public Result<Boolean> delete(String url) {
        String fileId = extractPublicFileId(url);
        if (StringUtils.isNotBlank(fileId)) {
            FileDetail fileDetail = fileDetailService.getById(fileId);
            if (fileDetail != null) {
                fileStorageService.delete(fileDetail.getUrl());
                fileDetailService.removeById(fileId);
                redisUtil.delete(RedisConstants.FILE_VIEW_CACHE_KEY + fileId);
            }
            return Result.success(true);
        }

        boolean flag = fileStorageService.delete(url);
        if (flag) {
            fileDetailService.delete(url);
        }
        return Result.success(flag);
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        long maxBytes = Math.max(1L, maxUploadSizeMb) * MB;
        if (file.getSize() > maxBytes) {
            throw new ServiceException("上传文件大小不能超过 " + maxUploadSizeMb + "MB");
        }
    }

    private String normalizeSource(String source) {
        if (StringUtils.isBlank(source)) {
            return "default";
        }
        String normalized = source.trim();
        if (!SOURCE_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException("非法上传来源");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private DetectedImageType detectImageType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[16];
            int length = inputStream.read(header);
            if (length < 12) {
                throw new ServiceException("不支持的文件格式");
            }

            if (length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return new DetectedImageType("jpg");
            }

            byte[] pngSignature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (length >= pngSignature.length && Arrays.equals(Arrays.copyOf(header, pngSignature.length), pngSignature)) {
                return new DetectedImageType("png");
            }

            String gifHeader = new String(header, 0, Math.min(length, 6), StandardCharsets.US_ASCII);
            if ("GIF87a".equals(gifHeader) || "GIF89a".equals(gifHeader)) {
                return new DetectedImageType("gif");
            }

            String riffHeader = new String(header, 0, 4, StandardCharsets.US_ASCII);
            String webpHeader = new String(header, 8, 4, StandardCharsets.US_ASCII);
            if ("RIFF".equals(riffHeader) && "WEBP".equals(webpHeader)) {
                return new DetectedImageType("webp");
            }

            throw new ServiceException("仅允许上传 jpg/png/gif/webp 图片");
        } catch (IOException ex) {
            throw new ServiceException("读取上传文件失败");
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
            String targetUrl = resolveFileTarget(fileDetailService.getById(fileId));
            if (StringUtils.isNotBlank(targetUrl)) {
                redisUtil.set(RedisConstants.FILE_VIEW_CACHE_KEY + fileId, targetUrl, RedisConstants.DAY_EXPIRE, TimeUnit.SECONDS);
            }
            return getPublicFileContentPrefix() + fileId;
        }

        return fileInfo.getUrl();
    }

    private String extractPublicFileId(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        String normalized = url.trim();
        String[] prefixes = {
                getPublicFileContentPrefix(),
                "/boylu/file/content/",
                "/boylu/file/view/",
                "/mojian/file/content/",
                "/mojian/file/view/",
                "/file/content/",
                "/file/view/"
        };

        for (String prefix : prefixes) {
            int index = normalized.indexOf(prefix);
            if (index >= 0) {
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
        }

        return null;
    }

    private void redirectToFile(String id, HttpServletResponse response) throws IOException {
        String cacheKey = RedisConstants.FILE_VIEW_CACHE_KEY + id;
        String targetUrl = null;
        try {
            Object cacheValue = redisUtil.get(cacheKey);
            if (cacheValue instanceof String) {
                targetUrl = (String) cacheValue;
            } else if (cacheValue != null) {
                targetUrl = String.valueOf(cacheValue);
            }
        } catch (Exception ex) {
            redisUtil.delete(cacheKey);
        }
        if (StringUtils.isBlank(targetUrl)) {
            FileDetail fileDetail = fileDetailService.getById(id);
            if (fileDetail == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            targetUrl = resolveFileTarget(fileDetail);
            if (StringUtils.isBlank(targetUrl)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            redisUtil.set(cacheKey, targetUrl, RedisConstants.DAY_EXPIRE, TimeUnit.SECONDS);
        }
        response.setHeader("Cache-Control", "public, max-age=2592000");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", targetUrl);
    }

    private String resolveFileTarget(FileDetail fileDetail) {
        if (fileDetail == null) {
            return null;
        }

        String targetUrl = normalizeStoredUrl(fileDetail.getUrl());
        if (StringUtils.isNotBlank(targetUrl)) {
            return targetUrl;
        }

        targetUrl = normalizeStoredUrl(fileDetail.getThUrl());
        if (StringUtils.isNotBlank(targetUrl)) {
            return targetUrl;
        }

        if (StringUtils.isNotBlank(fileDetail.getPath())) {
            return buildLocalFileUrl(fileDetail.getBasePath(), fileDetail.getPath(), fileDetail.getFilename());
        }

        return null;
    }

    private String normalizeStoredUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (url.contains("/localFile/")) {
            return url.substring(url.indexOf("/localFile/"));
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/localFile/")) {
            return url;
        }
        return null;
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

    private String buildLocalFileUrl(String basePath, String path, String filename) {
        StringBuilder builder = new StringBuilder("/localFile/");
        if (StringUtils.isNotBlank(basePath)) {
            builder.append(trimSlashes(basePath)).append("/");
        }
        if (StringUtils.isNotBlank(path)) {
            builder.append(trimLeadingSlash(path));
        }
        if (StringUtils.isNotBlank(filename) && !builder.toString().endsWith(filename)) {
            if (!builder.toString().endsWith("/")) {
                builder.append("/");
            }
            builder.append(filename);
        }
        return builder.toString().replace("//", "/");
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

    private static class DetectedImageType {
        private final String extension;

        private DetectedImageType(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }
}
