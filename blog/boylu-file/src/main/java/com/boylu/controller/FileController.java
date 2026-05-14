package com.boylu.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.common.Constants;
import com.boylu.common.RedisConstants;
import com.boylu.common.Result;
import com.boylu.entity.FileDetail;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysFileOss;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.service.ArticleCoverImageService;
import com.boylu.service.FileDetailService;
import com.boylu.utils.CoverImageUtil;
import com.boylu.utils.DateUtil;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.article.CoverImageVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private static final String DEFAULT_PUBLIC_FILE_CONTENT_PREFIX = "/boylu/file/content/";
    private static final String LOCAL_FILE_PREFIX = "/localFile/";
    private static final String IMG_CACHE_DIR = "img-cache";
    private static final String CHUNK_META_PREFIX = "file:chunk:meta:";
    private static final String CHUNK_PART_SET_PREFIX = "file:chunk:parts:";
    private static final String CHUNK_HASH_PREFIX = "file:chunk:hash:";
    private static final long MB = 1024L * 1024L;
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");

    private final FileDetailService fileDetailService;

    private final FileStorageService fileStorageService;

    private final RedisUtil redisUtil;

    private final ArticleCoverImageService articleCoverImageService;

    private final SysArticleMapper sysArticleMapper;


    @Value("${app.file.public-prefix:" + DEFAULT_PUBLIC_FILE_CONTENT_PREFIX + "}")
    private String publicFileContentPrefix;

    @Value("${app.file.max-upload-size-mb:30}")
    private long maxUploadSizeMb;

    @Value("${app.file.chunk-size-mb:2}")
    private long chunkSizeMb;

    @Value("${app.file.chunk-expire-hours:24}")
    private long chunkExpireHours;

    @Value("${app.file.max-image-width:6000}")
    private int maxImageWidth;

    @Value("${app.file.max-image-height:6000}")
    private int maxImageHeight;

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
    public Result<?> upload(MultipartFile file, String source) {
        validateUploadFile(file);
        String normalizedSource = normalizeSource(source);
        byte[] bytes = readUploadBytes(file);
        return Result.success(uploadImageBytes(bytes, file.getOriginalFilename(), file.getContentType(), normalizedSource));
    }

    @SaCheckLogin
    @PostMapping("/upload/chunk/init")
    @ApiOperation(value = "初始化分片上传")
    public Result<Map<String, Object>> initChunkUpload(@RequestParam String fileName,
                                                       @RequestParam Long totalSize,
                                                       @RequestParam Integer totalChunks,
                                                       @RequestParam(required = false) String fileHash,
                                                       @RequestParam(required = false) String source) throws IOException {
        if (StringUtils.isBlank(fileName) || totalSize == null || totalSize <= 0 || totalChunks == null || totalChunks <= 0) {
            throw new ServiceException("分片上传参数不合法");
        }
        long maxBytes = Math.max(1L, maxUploadSizeMb) * MB;
        if (totalSize > maxBytes) {
            throw new ServiceException("上传文件大小不能超过 " + maxUploadSizeMb + "MB");
        }
        String normalizedSource = normalizeSource(source);
        String normalizedHash = StringUtils.isBlank(fileHash) ? null : fileHash.trim();
        long chunkSizeBytes = Math.max(1L, chunkSizeMb) * MB;
        long expectedChunks = (totalSize + chunkSizeBytes - 1) / chunkSizeBytes;
        if (Math.abs(expectedChunks - totalChunks.longValue()) > 1) {
            throw new ServiceException("分片数量与文件大小不匹配");
        }

        String uploadId = null;
        if (StringUtils.isNotBlank(normalizedHash)) {
            Object existing = redisUtil.get(CHUNK_HASH_PREFIX + normalizedHash);
            if (existing != null) {
                uploadId = String.valueOf(existing);
            }
        }
        if (StringUtils.isBlank(uploadId) || !Boolean.TRUE.equals(redisUtil.hasKey(CHUNK_META_PREFIX + uploadId))) {
            uploadId = UUID.randomUUID().toString().replace("-", "");
        }

        Map<Object, Object> meta = new HashMap<Object, Object>();
        meta.put("fileName", fileName.trim());
        meta.put("totalSize", totalSize.toString());
        meta.put("totalChunks", totalChunks.toString());
        meta.put("source", normalizedSource);
        meta.put("chunkSize", String.valueOf(chunkSizeBytes));
        if (StringUtils.isNotBlank(normalizedHash)) {
            meta.put("fileHash", normalizedHash);
        }
        redisUtil.hSetAll(CHUNK_META_PREFIX + uploadId, meta);
        redisUtil.expire(CHUNK_META_PREFIX + uploadId, Math.max(1L, chunkExpireHours), TimeUnit.HOURS);
        redisUtil.expire(CHUNK_PART_SET_PREFIX + uploadId, Math.max(1L, chunkExpireHours), TimeUnit.HOURS);
        if (StringUtils.isNotBlank(normalizedHash)) {
            redisUtil.set(CHUNK_HASH_PREFIX + normalizedHash, uploadId, Math.max(1L, chunkExpireHours), TimeUnit.HOURS);
        }

        Files.createDirectories(resolveChunkUploadDir(uploadId));
        Set<Object> uploadedSet = redisUtil.sMembers(CHUNK_PART_SET_PREFIX + uploadId);
        List<Integer> uploadedChunks = parseChunkIndexSet(uploadedSet);

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("uploadId", uploadId);
        response.put("chunkSize", chunkSizeBytes);
        response.put("uploadedChunks", uploadedChunks);
        response.put("concurrency", 4);
        return Result.success(response);
    }

    @SaCheckLogin
    @PostMapping("/upload/chunk/part")
    @ApiOperation(value = "上传分片")
    public Result<Boolean> uploadChunkPart(@RequestParam String uploadId,
                                           @RequestParam Integer chunkIndex,
                                           @RequestParam MultipartFile file) throws IOException {
        if (StringUtils.isBlank(uploadId) || chunkIndex == null || chunkIndex < 0 || file == null || file.isEmpty()) {
            throw new ServiceException("分片上传参数不合法");
        }
        Map<Object, Object> meta = redisUtil.hGetAll(CHUNK_META_PREFIX + uploadId);
        if (meta == null || meta.isEmpty()) {
            throw new ServiceException("上传会话不存在或已过期，请重新上传");
        }
        int totalChunks = parsePositiveInt(meta.get("totalChunks"), 1);
        if (chunkIndex >= totalChunks) {
            throw new ServiceException("分片索引越界");
        }
        long chunkSizeBytes = parsePositiveLong(meta.get("chunkSize"), Math.max(1L, chunkSizeMb) * MB);
        if (file.getSize() > chunkSizeBytes + (256 * 1024)) {
            throw new ServiceException("分片大小超过限制");
        }
        Path uploadDir = resolveChunkUploadDir(uploadId);
        Files.createDirectories(uploadDir);
        Path partPath = uploadDir.resolve("part-" + chunkIndex + ".tmp");
        file.transferTo(partPath.toFile());
        redisUtil.sAdd(CHUNK_PART_SET_PREFIX + uploadId, String.valueOf(chunkIndex));
        redisUtil.expire(CHUNK_META_PREFIX + uploadId, Math.max(1L, chunkExpireHours), TimeUnit.HOURS);
        redisUtil.expire(CHUNK_PART_SET_PREFIX + uploadId, Math.max(1L, chunkExpireHours), TimeUnit.HOURS);
        return Result.success(true);
    }

    @SaCheckLogin
    @PostMapping("/upload/chunk/complete")
    @ApiOperation(value = "完成分片上传")
    public Result<?> completeChunkUpload(@RequestParam String uploadId) throws IOException {
        if (StringUtils.isBlank(uploadId)) {
            throw new ServiceException("上传会话不存在");
        }
        String metaKey = CHUNK_META_PREFIX + uploadId;
        Map<Object, Object> meta = redisUtil.hGetAll(metaKey);
        if (meta == null || meta.isEmpty()) {
            throw new ServiceException("上传会话不存在或已过期，请重新上传");
        }

        String fileName = String.valueOf(meta.getOrDefault("fileName", "upload-image"));
        String source = String.valueOf(meta.getOrDefault("source", "default"));
        int totalChunks = parsePositiveInt(meta.get("totalChunks"), 1);
        long totalSize = parsePositiveLong(meta.get("totalSize"), 0L);

        Set<Object> uploadedSet = redisUtil.sMembers(CHUNK_PART_SET_PREFIX + uploadId);
        if (uploadedSet == null || uploadedSet.size() < totalChunks) {
            throw new ServiceException("分片不完整，请继续上传后重试");
        }

        Path uploadDir = resolveChunkUploadDir(uploadId);
        if (!Files.isDirectory(uploadDir)) {
            throw new ServiceException("上传分片不存在，请重新上传");
        }

        Path mergedPath = uploadDir.resolve("merged-image.tmp");
        try {
            Files.deleteIfExists(mergedPath);
            try (java.io.OutputStream outputStream = Files.newOutputStream(
                    mergedPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                for (int i = 0; i < totalChunks; i++) {
                    Path part = uploadDir.resolve("part-" + i + ".tmp");
                    if (!Files.isRegularFile(part)) {
                        throw new ServiceException("缺少分片 " + i + "，请重新上传");
                    }
                    Files.copy(part, outputStream);
                }
            }

            long mergedSize = Files.size(mergedPath);
            if (totalSize > 0 && Math.abs(mergedSize - totalSize) > 1024) {
                throw new ServiceException("上传文件校验失败，请重新上传");
            }
            Object result = uploadImageFile(mergedPath, fileName, Files.probeContentType(mergedPath), source);
            cleanupChunkUpload(uploadId, String.valueOf(meta.get("fileHash")));
            return Result.success(result);
        } finally {
            Files.deleteIfExists(mergedPath);
        }
    }

    @SaCheckLogin
    @PostMapping("/upload/chunk/abort")
    @ApiOperation(value = "取消分片上传")
    public Result<Boolean> abortChunkUpload(@RequestParam String uploadId) throws IOException {
        if (StringUtils.isBlank(uploadId)) {
            return Result.success(true);
        }
        String fileHash = null;
        Map<Object, Object> meta = redisUtil.hGetAll(CHUNK_META_PREFIX + uploadId);
        if (meta != null && !meta.isEmpty() && meta.get("fileHash") != null) {
            fileHash = String.valueOf(meta.get("fileHash"));
        }
        cleanupChunkUpload(uploadId, fileHash);
        return Result.success(true);
    }

    @GetMapping("/content/{id}")
    @ApiOperation(value = "根据文件 ID 访问文件")
    public void content(@PathVariable String id, HttpServletResponse response) throws IOException {
        writeFileById(id, response);
    }

    @GetMapping("/view/{id}")
    @ApiOperation(value = "根据文件 ID 预览文件")
    public void view(@PathVariable String id, HttpServletResponse response) throws IOException {
        writeFileById(id, response);
    }

    @GetMapping("/by-url")
    @ApiOperation(value = "兼容旧本地文件地址")
    public void byUrl(String url, HttpServletResponse response) throws IOException {
        String normalizedUrl = normalizeRequestUrl(url);
        if (StringUtils.isBlank(normalizedUrl) || !normalizedUrl.startsWith(LOCAL_FILE_PREFIX)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        FileDetail fileDetail = findFileDetailByUrl(normalizedUrl);
        if (fileDetail != null) {
            writeFile(fileDetail, response);
            return;
        }

        Path filePath = resolveLocalFilePathFromUrl(normalizedUrl);
        if (filePath == null || !Files.isRegularFile(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        writeLocalFile(filePath, null, response);
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

        String normalizedUrl = normalizeRequestUrl(url);
        if (StringUtils.isNotBlank(normalizedUrl) && normalizedUrl.startsWith(LOCAL_FILE_PREFIX)) {
            FileDetail fileDetail = findFileDetailByUrl(normalizedUrl);
            if (fileDetail != null) {
                fileStorageService.delete(fileDetail.getUrl());
                fileDetailService.removeById(fileDetail.getId());
                redisUtil.delete(RedisConstants.FILE_VIEW_CACHE_KEY + fileDetail.getId());
                return Result.success(true);
            }
        }

        boolean flag = fileStorageService.delete(url);
        if (flag) {
            fileDetailService.delete(url);
        }
        return Result.success(flag);
    }

    @SaCheckLogin
    @PostMapping("/article-cover/backfill-home")
    @SaCheckPermission("sys:article:update")
    @ApiOperation(value = "backfill home article cover image metadata")
    public Result<Integer> backfillHomeArticleCovers(@RequestParam(defaultValue = "20") Integer limit) throws IOException {
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        List<SysArticle> articles = sysArticleMapper.selectList(new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getTitle, SysArticle::getCover,
                        SysArticle::getCoverImage, SysArticle::getIsStick, SysArticle::getCreateTime)
                .eq(SysArticle::getStatus, Constants.YES)
                .orderByDesc(SysArticle::getIsStick)
                .orderByDesc(SysArticle::getCreateTime)
                .last("limit " + safeLimit));

        int updated = 0;
        for (SysArticle article : articles) {
            if (article == null || StringUtils.isBlank(article.getCover())) {
                continue;
            }

            CoverImageVo current = CoverImageUtil.fromJson(article.getCoverImage(), article.getCover(), article.getTitle());
            if (hasResponsiveVariants(current)) {
                continue;
            }
            if (isDefaultFallback(current)) {
                continue;
            }

            Path sourcePath = resolveCoverSourcePath(article.getCover());
            if (sourcePath == null || !Files.isRegularFile(sourcePath)) {
                log.warn("Skip article cover backfill because source file is missing, articleId={}, cover={}",
                        article.getId(), article.getCover());
                continue;
            }
            if (!isSafeLocalImageForProcessing(sourcePath)) {
                log.warn("Skip article cover backfill because source image is too large or invalid, articleId={}, cover={}",
                        article.getId(), article.getCover());
                continue;
            }

            String contentType = Files.probeContentType(sourcePath);
            String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/article-cover/";
            CoverImageVo coverImage = articleCoverImageService.process(
                    sourcePath,
                    sourcePath.getFileName().toString(),
                    contentType,
                    path,
                    "article-cover"
            );
            coverImage.setAlt(StringUtils.defaultString(article.getTitle()));

            SysArticle update = new SysArticle();
            update.setId(article.getId());
            update.setCover(coverImage.getFallback());
            update.setCoverImage(CoverImageUtil.toJson(coverImage));
            sysArticleMapper.updateById(update);
            updated++;
        }

        if (updated > 0) {
            redisUtil.delete(RedisConstants.HOME_POSTS_CACHE_KEY);
        }
        return Result.success(updated);
    }

    @SaCheckLogin
    @GetMapping("/img-cache/stats")
    @ApiOperation(value = "img-cache 缂撳瓨缁熻")
    public Result<Map<String, Object>> imgCacheStats() throws IOException {
        return Result.success(scanImgCache(0, false));
    }

    @SaCheckLogin
    @PostMapping("/img-cache/cleanup")
    @SaCheckPermission("sys:file:delete")
    @ApiOperation(value = "瀹夊叏娓呯悊 img-cache 缂撳瓨")
    public Result<Map<String, Object>> cleanupImgCache(@RequestParam(defaultValue = "90") Integer maxAgeDays,
                                                       @RequestParam(defaultValue = "false") Boolean dryRun) throws IOException {
        int safeMaxAgeDays = Math.max(7, Math.min(maxAgeDays == null ? 90 : maxAgeDays, 3650));
        Map<String, Object> result = scanImgCache(safeMaxAgeDays, !Boolean.TRUE.equals(dryRun));
        result.put("maxAgeDays", safeMaxAgeDays);
        result.put("dryRun", Boolean.TRUE.equals(dryRun));
        return Result.success(result);
    }

    private boolean hasResponsiveVariants(CoverImageVo coverImage) {
        return coverImage != null
                && coverImage.getVariants() != null
                && coverImage.getVariants().values().stream()
                .anyMatch(variants -> variants != null && !variants.isEmpty());
    }

    private boolean isDefaultFallback(CoverImageVo coverImage) {
        return coverImage != null && StringUtils.equals(coverImage.getFallback(), CoverImageUtil.FALLBACK_IMAGE);
    }

    private CoverImageVo buildFallbackCoverImage(String title) {
        return CoverImageVo.builder()
                .key(CoverImageUtil.FALLBACK_IMAGE)
                .alt(StringUtils.defaultString(title))
                .width(1200)
                .height(900)
                .dominantColor(CoverImageUtil.DEFAULT_DOMINANT_COLOR)
                .fallback(CoverImageUtil.FALLBACK_IMAGE)
                .hash("fallback")
                .build();
    }

    private Map<String, Object> scanImgCache(int maxAgeDays, boolean deleteExpired) throws IOException {
        Path cacheRoot = resolveImgCacheRoot();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("cacheRoot", cacheRoot.toString());
        result.put("fileCount", 0L);
        result.put("totalBytes", 0L);
        result.put("expiredCount", 0L);
        result.put("expiredBytes", 0L);
        result.put("deletedCount", 0L);
        result.put("deletedBytes", 0L);

        if (!Files.isDirectory(cacheRoot)) {
            return result;
        }

        long cutoffMillis = maxAgeDays > 0
                ? Instant.now().minusSeconds(maxAgeDays * 24L * 60L * 60L).toEpochMilli()
                : Long.MIN_VALUE;
        List<Path> visitedDirs = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(cacheRoot)) {
            for (Path path : stream.collect(Collectors.toList())) {
                if (!path.normalize().startsWith(cacheRoot)) {
                    continue;
                }
                if (Files.isDirectory(path)) {
                    visitedDirs.add(path);
                    continue;
                }
                if (!Files.isRegularFile(path) || path.getFileName().toString().endsWith(".tmp")) {
                    continue;
                }
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                long size = attributes.size();
                incrementResult(result, "fileCount", 1L);
                incrementResult(result, "totalBytes", size);
                boolean expired = maxAgeDays > 0 && attributes.lastModifiedTime().toMillis() < cutoffMillis;
                if (!expired) {
                    continue;
                }
                incrementResult(result, "expiredCount", 1L);
                incrementResult(result, "expiredBytes", size);
                if (deleteExpired) {
                    Files.deleteIfExists(path);
                    incrementResult(result, "deletedCount", 1L);
                    incrementResult(result, "deletedBytes", size);
                }
            }
        }

        if (deleteExpired) {
            Collections.sort(visitedDirs, (left, right) -> right.getNameCount() - left.getNameCount());
            for (Path dir : visitedDirs) {
                if (dir.equals(cacheRoot) || !dir.normalize().startsWith(cacheRoot)) {
                    continue;
                }
                try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                    if (!children.iterator().hasNext()) {
                        Files.deleteIfExists(dir);
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    private void incrementResult(Map<String, Object> result, String key, long delta) {
        Object value = result.get(key);
        long current = value == null ? 0L : Long.parseLong(String.valueOf(value));
        result.put(key, current + delta);
    }

    private Path resolveImgCacheRoot() {
        Path rootPath = getLocalStorageRootPath(null);
        Path cacheRoot = rootPath.resolve(IMG_CACHE_DIR).normalize();
        if (!cacheRoot.startsWith(rootPath)) {
            throw new ServiceException("img-cache path is unsafe");
        }
        return cacheRoot;
    }

    private Path resolveCoverSourcePath(String coverUrl) {
        String fileId = extractPublicFileId(coverUrl);
        if (StringUtils.isNotBlank(fileId)) {
            FileDetail fileDetail = fileDetailService.getById(fileId);
            if (fileDetail != null) {
                return resolveLocalFilePath(fileDetail);
            }
        }

        String normalizedUrl = normalizeRequestUrl(coverUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return null;
        }

        if (normalizedUrl.startsWith(LOCAL_FILE_PREFIX)) {
            return resolveLocalFilePathFromUrl(normalizedUrl);
        }
        return null;
    }

    private boolean isSafeLocalImageForProcessing(Path sourcePath) {
        try {
            long maxBytes = Math.max(1L, maxUploadSizeMb) * MB;
            if (Files.size(sourcePath) > maxBytes) {
                return false;
            }
            validateImageDimensions(sourcePath);
            return true;
        } catch (Exception ex) {
            return false;
        }
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

    private DetectedImageType detectImageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new ServiceException("不支持的文件格式");
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new DetectedImageType("jpg");
        }
        byte[] pngSignature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length >= pngSignature.length && Arrays.equals(Arrays.copyOf(bytes, pngSignature.length), pngSignature)) {
            return new DetectedImageType("png");
        }
        String gifHeader = new String(bytes, 0, Math.min(bytes.length, 6), StandardCharsets.US_ASCII);
        if ("GIF87a".equals(gifHeader) || "GIF89a".equals(gifHeader)) {
            return new DetectedImageType("gif");
        }
        String riffHeader = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        String webpHeader = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
        if ("RIFF".equals(riffHeader) && "WEBP".equals(webpHeader)) {
            return new DetectedImageType("webp");
        }
        throw new ServiceException("仅允许上传 jpg/png/gif/webp 图片");
    }

    private DetectedImageType detectImageType(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] header = new byte[16];
            int length = inputStream.read(header);
            if (length < 12) {
                throw new ServiceException("Unsupported file format");
            }
            return detectImageType(Arrays.copyOf(header, length));
        } catch (IOException ex) {
            throw new ServiceException("Failed to read upload file");
        }
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ServiceException("读取上传文件失败");
        }
    }

    private Object uploadImageBytes(byte[] bytes, String originalFilename, String contentType, String normalizedSource) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("上传文件不能为空");
        }
        long maxBytes = Math.max(1L, maxUploadSizeMb) * MB;
        if (bytes.length > maxBytes) {
            throw new ServiceException("上传文件大小不能超过 " + maxUploadSizeMb + "MB");
        }
        validateImageDimensions(bytes);
        DetectedImageType imageType = detectImageType(bytes);
        String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/";
        if (StringUtils.isNotBlank(normalizedSource)) {
            path = path + normalizedSource + "/";
        }
        if ("article-cover".equals(normalizedSource)) {
            return articleCoverImageService.process(bytes, originalFilename, contentType, path, normalizedSource);
        }
        String saveFilename = UUID.randomUUID().toString().replace("-", "") + "." + imageType.getExtension();
        MultipartFile multipartFile = new BytesMultipartFile("file", saveFilename,
                StringUtils.defaultIfBlank(contentType, "image/" + imageType.getExtension()), bytes);
        FileInfo fileInfo = fileStorageService.of(multipartFile)
                .setPath(path)
                .setSaveFilename(saveFilename)
                .putAttr("source", normalizedSource)
                .upload();
        if (fileInfo == null) {
            throw new ServiceException("上传文件失败");
        }
        return buildPublicContentUrl(fileInfo);
    }

    private Object uploadImageFile(Path filePath, String originalFilename, String contentType, String normalizedSource) {
        try {
            if (filePath == null || !Files.isRegularFile(filePath)) {
                throw new ServiceException("Upload file cannot be empty");
            }
            long size = Files.size(filePath);
            long maxBytes = Math.max(1L, maxUploadSizeMb) * MB;
            if (size <= 0) {
                throw new ServiceException("Upload file cannot be empty");
            }
            if (size > maxBytes) {
                throw new ServiceException("Upload file size cannot exceed " + maxUploadSizeMb + "MB");
            }

            validateImageDimensions(filePath);
            DetectedImageType imageType = detectImageType(filePath);
            String path = DateUtil.parseDateToStr(DateUtil.YYYYMMDD, DateUtil.getNowDate()) + "/";
            if (StringUtils.isNotBlank(normalizedSource)) {
                path = path + normalizedSource + "/";
            }
            String resolvedContentType = StringUtils.defaultIfBlank(contentType, "image/" + imageType.getExtension());
            if ("article-cover".equals(normalizedSource)) {
                return articleCoverImageService.process(filePath, originalFilename, resolvedContentType, path, normalizedSource);
            }

            String saveFilename = UUID.randomUUID().toString().replace("-", "") + "." + imageType.getExtension();
            FileInfo fileInfo = fileStorageService.of(filePath.toFile(), saveFilename, resolvedContentType, size)
                    .setPath(path)
                    .setSaveFilename(saveFilename)
                    .setContentType(resolvedContentType)
                    .setOriginalFilename(StringUtils.defaultIfBlank(originalFilename, saveFilename))
                    .putAttr("source", normalizedSource)
                    .upload();
            if (fileInfo == null) {
                throw new ServiceException("Upload file failed");
            }
            return buildPublicContentUrl(fileInfo);
        } catch (IOException ex) {
            throw new ServiceException("Failed to read upload file");
        }
    }

    private void validateImageDimensions(Path filePath) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(filePath.toFile())) {
            validateImageDimensions(imageInputStream);
        } catch (IOException ex) {
            throw new ServiceException("读取上传图片失败");
        }
    }

    private void validateImageDimensions(ImageInputStream imageInputStream) throws IOException {
        if (imageInputStream == null) {
            throw new ServiceException("无法解析上传图片");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        if (!readers.hasNext()) {
            throw new ServiceException("无法解析上传图片");
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(imageInputStream, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width > Math.max(1, maxImageWidth) || height > Math.max(1, maxImageHeight)) {
                throw new ServiceException("图片分辨率超过限制，最大支持 "
                        + Math.max(1, maxImageWidth) + "x" + Math.max(1, maxImageHeight));
            }
        } finally {
            reader.dispose();
        }
    }

    private void validateImageDimensions(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ServiceException("无法解析上传图片");
            }
            if (image.getWidth() > Math.max(1, maxImageWidth) || image.getHeight() > Math.max(1, maxImageHeight)) {
                throw new ServiceException("图片分辨率超过限制，最大支持 "
                        + Math.max(1, maxImageWidth) + "x" + Math.max(1, maxImageHeight));
            }
        } catch (IOException ex) {
            throw new ServiceException("读取上传图片失败");
        }
    }

    private Path resolveChunkUploadDir(String uploadId) {
        Path rootPath = getLocalStorageRootPath(null);
        return rootPath.resolve("_chunks").resolve(uploadId).normalize();
    }

    private List<Integer> parseChunkIndexSet(Set<Object> uploadedSet) {
        if (uploadedSet == null || uploadedSet.isEmpty()) {
            return new ArrayList<Integer>();
        }
        return uploadedSet.stream()
                .map(value -> parsePositiveInt(value, -1))
                .filter(index -> index >= 0)
                .sorted()
                .collect(Collectors.toList());
    }

    private int parsePositiveInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value));
            return parsed >= 0 ? parsed : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private long parsePositiveLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed >= 0 ? parsed : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void cleanupChunkUpload(String uploadId, String fileHash) throws IOException {
        redisUtil.delete(CHUNK_META_PREFIX + uploadId);
        redisUtil.delete(CHUNK_PART_SET_PREFIX + uploadId);
        if (StringUtils.isNotBlank(fileHash) && !"null".equalsIgnoreCase(fileHash.trim())) {
            redisUtil.delete(CHUNK_HASH_PREFIX + fileHash.trim());
        }
        Path uploadDir = resolveChunkUploadDir(uploadId);
        if (Files.isDirectory(uploadDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
                for (Path path : stream) {
                    Files.deleteIfExists(path);
                }
            } catch (IOException ex) {
                log.warn("Failed to cleanup chunk upload files, uploadId={}", uploadId, ex);
            }
            Files.deleteIfExists(uploadDir);
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
            FileDetail fileDetail = fileDetailService.getById(fileId);
            if (fileDetail != null) {
                String targetUrl = resolveFileTarget(fileDetail);
                if (StringUtils.isNotBlank(targetUrl)) {
                    redisUtil.set(RedisConstants.FILE_VIEW_CACHE_KEY + fileId, targetUrl, RedisConstants.DAY_EXPIRE, TimeUnit.SECONDS);
                }
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

    private void writeFileById(String id, HttpServletResponse response) throws IOException {
        FileDetail fileDetail = fileDetailService.getById(id);
        if (fileDetail == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        writeFile(fileDetail, response);
    }

    private void writeFile(FileDetail fileDetail, HttpServletResponse response) throws IOException {
        if (isLocalFile(fileDetail)) {
            String targetUrl = resolveCachedFileTarget(fileDetail.getId(), fileDetail);
            if (StringUtils.isNotBlank(targetUrl) && targetUrl.startsWith(LOCAL_FILE_PREFIX)) {
                redirectToTarget(targetUrl, response);
                return;
            }

            Path filePath = resolveLocalFilePath(fileDetail);
            if (filePath != null && Files.isRegularFile(filePath)) {
                writeLocalFile(filePath, fileDetail.getContentType(), response);
                return;
            }
        }

        String targetUrl = resolveCachedFileTarget(fileDetail.getId(), fileDetail);
        if (StringUtils.isBlank(targetUrl)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (targetUrl.startsWith(LOCAL_FILE_PREFIX)) {
            Path filePath = resolveLocalFilePathFromUrl(targetUrl);
            if (filePath != null && Files.isRegularFile(filePath)) {
                writeLocalFile(filePath, fileDetail.getContentType(), response);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        redirectToTarget(targetUrl, response);
    }

    private void redirectToTarget(String targetUrl, HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", targetUrl);
    }

    private String resolveCachedFileTarget(String id, FileDetail fileDetail) {
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
            targetUrl = resolveFileTarget(fileDetail);
            if (StringUtils.isNotBlank(targetUrl)) {
                redisUtil.set(cacheKey, targetUrl, RedisConstants.DAY_EXPIRE, TimeUnit.SECONDS);
            }
        }
        return targetUrl;
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
        StringBuilder builder = new StringBuilder(LOCAL_FILE_PREFIX);
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

    private FileDetail findFileDetailByUrl(String url) {
        return fileDetailService.getOne(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getUrl, url)
                .last("limit 1"));
    }

    private boolean isLocalFile(FileDetail fileDetail) {
        if (fileDetail == null) {
            return false;
        }
        return "local".equalsIgnoreCase(fileDetail.getPlatform())
                || StringUtils.containsIgnoreCase(fileDetail.getUrl(), LOCAL_FILE_PREFIX)
                || StringUtils.containsIgnoreCase(fileDetail.getThUrl(), LOCAL_FILE_PREFIX);
    }

    private Path resolveLocalFilePath(FileDetail fileDetail) {
        Path rootPath = getLocalStorageRootPath(fileDetail);
        if (rootPath == null) {
            return null;
        }

        Path filePath = buildSafeLocalPath(
                rootPath,
                joinRelativePath(fileDetail.getBasePath(), fileDetail.getPath(), fileDetail.getFilename())
        );
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

    private void writeLocalFile(Path filePath, String contentType, HttpServletResponse response) throws IOException {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String resolvedContentType = StringUtils.defaultIfBlank(contentType, Files.probeContentType(filePath));
        if (StringUtils.isBlank(resolvedContentType)) {
            resolvedContentType = "application/octet-stream";
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(resolvedContentType);
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Length", String.valueOf(Files.size(filePath)));
        Files.copy(filePath, response.getOutputStream());
    }

    private String normalizeRequestUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return normalizeStoredUrl(URLDecoder.decode(url.trim(), StandardCharsets.UTF_8.name()));
        } catch (Exception ex) {
            return normalizeStoredUrl(url.trim());
        }
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
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }
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
