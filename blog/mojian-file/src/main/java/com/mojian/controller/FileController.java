package com.mojian.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mojian.common.Constants;
import com.mojian.common.RedisConstants;
import com.mojian.common.Result;
import com.mojian.entity.FileDetail;
import com.mojian.entity.SysFileOss;
import com.mojian.exception.ServiceException;
import com.mojian.service.FileDetailService;
import com.mojian.utils.DateUtil;
import com.mojian.utils.RedisUtil;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
@RequiredArgsConstructor
public class FileController {

    private static final String DEFAULT_PUBLIC_FILE_CONTENT_PREFIX = "/boylu/file/content/";

    private final FileDetailService fileDetailService;

    private final FileStorageService fileStorageService;

    private final RedisUtil redisUtil;

    @Value("${app.file.public-prefix:" + DEFAULT_PUBLIC_FILE_CONTENT_PREFIX + "}")
    private String publicFileContentPrefix;

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
        String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/";
        if (StringUtils.isNotBlank(source)) {
            path = path + source + "/";
        }

        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.substringAfterLast(originalFilename, ".");
        String saveFilename = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.isNotBlank(extension)) {
            saveFilename = saveFilename + "." + extension.toLowerCase();
        }

        FileInfo fileInfo = fileStorageService.of(file)
                .setPath(path)
                .setSaveFilename(saveFilename)
                .putAttr("source", source)
                .upload();

        if (fileInfo == null) {
            throw new ServiceException("上传文件失败");
        }
        return Result.success(buildPublicContentUrl(fileInfo));
    }

    @GetMapping("/content/{id}")
    @ApiOperation(value = "閫氳繃鏂囦欢 ID 璁块棶鏂囦欢")
    public void content(@PathVariable String id, HttpServletResponse response) throws IOException {
        redirectToFile(id, response);
    }

    @GetMapping("/view/{id}")
    @ApiOperation(value = "閫氳繃鏂囦欢 ID 棰勮鏂囦欢")
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
}
