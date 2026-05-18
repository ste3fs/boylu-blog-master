package com.boylu.service.notion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.entity.NotionArticleSyncLog;
import com.boylu.mapper.NotionArticleSyncLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionSyncLogService {

    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_SYNC = "sync";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String IMAGE_PENDING = "pending";
    public static final String IMAGE_RUNNING = "running";
    public static final String IMAGE_SUCCESS = "success";
    public static final String IMAGE_PARTIAL_FAILED = "partial_failed";
    public static final String IMAGE_FAILED = "failed";
    public static final String IMAGE_NONE = "none";
    public static final String IMAGE_SKIPPED = "skipped";

    private final NotionArticleSyncLogMapper notionArticleSyncLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(String action, Long articleId, String sourceUrl, String articleTitle) {
        try {
            NotionArticleSyncLog log = NotionArticleSyncLog.builder()
                    .articleId(articleId)
                    .articleTitle(shortText(articleTitle, 180))
                    .sourceUrl(shortText(sourceUrl, 500))
                    .action(action)
                    .status(STATUS_RUNNING)
                    .imageStatus(IMAGE_PENDING)
                    .message("Notion 同步处理中")
                    .build();
            notionArticleSyncLogMapper.insert(log);
            return log.getId();
        } catch (Exception ex) {
            log.warn("Failed to create Notion sync log", ex);
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSyncSuccess(Long logId, Long articleId, String articleTitle, String sourceUrl,
                                Integer importedBlocks, List<String> warnings) {
        if (logId == null) {
            return;
        }
        try {
            NotionArticleSyncLog update = new NotionArticleSyncLog();
            update.setId(logId);
            update.setArticleId(articleId);
            update.setArticleTitle(shortText(articleTitle, 180));
            update.setSourceUrl(shortText(sourceUrl, 500));
            update.setStatus(STATUS_SUCCESS);
            update.setImageStatus(IMAGE_PENDING);
            update.setImportedBlocks(importedBlocks);
            update.setMessage("文章同步成功，图片正在后台本地化");
            update.setWarnings(joinWarnings(warnings));
            notionArticleSyncLogMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("Failed to update Notion sync success log, logId={}", logId, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSyncFailed(Long logId, Long articleId, String articleTitle, String sourceUrl, Exception error) {
        if (logId == null) {
            return;
        }
        try {
            NotionArticleSyncLog update = new NotionArticleSyncLog();
            update.setId(logId);
            update.setArticleId(articleId);
            update.setArticleTitle(shortText(articleTitle, 180));
            update.setSourceUrl(shortText(sourceUrl, 500));
            update.setStatus(STATUS_FAILED);
            update.setImageStatus(IMAGE_SKIPPED);
            update.setMessage(shortText(error == null ? "Notion 同步失败" : error.getMessage(), 500));
            update.setErrorDetail(shortText(stackTrace(error), 4000));
            notionArticleSyncLogMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("Failed to update Notion sync failed log, logId={}", logId, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSyncSkipped(Long logId, Long articleId, String articleTitle, String sourceUrl, String message) {
        if (logId == null) {
            return;
        }
        try {
            NotionArticleSyncLog update = new NotionArticleSyncLog();
            update.setId(logId);
            update.setArticleId(articleId);
            update.setArticleTitle(shortText(articleTitle, 180));
            update.setSourceUrl(shortText(sourceUrl, 500));
            update.setStatus(STATUS_SKIPPED);
            update.setImageStatus(IMAGE_SKIPPED);
            update.setImportedBlocks(0);
            update.setChangedFields(0);
            update.setTotalImages(0);
            update.setLocalizedImages(0);
            update.setFailedImages(0);
            update.setMessage(shortText(message, 500));
            notionArticleSyncLogMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("Failed to update Notion sync skipped log, logId={}", logId, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markImageRunning(Long logId) {
        updateImageStatus(logId, IMAGE_RUNNING, "图片正在后台下载到本站", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markImageResult(Long logId, NotionImportService.LocalizeImagesResult result) {
        if (logId == null || result == null) {
            return;
        }
        int total = safeInt(result.getTotalImages());
        int localized = safeInt(result.getLocalizedImages());
        int failed = safeInt(result.getFailedImages());
        String imageStatus;
        String message;
        if (total <= 0) {
            imageStatus = IMAGE_NONE;
            message = "没有需要本地化的远程图片";
        } else if (failed <= 0) {
            imageStatus = IMAGE_SUCCESS;
            message = "图片本地化完成：" + localized + "/" + total;
        } else if (localized > 0) {
            imageStatus = IMAGE_PARTIAL_FAILED;
            message = "图片部分本地化失败：" + localized + "/" + total + "，失败 " + failed;
        } else {
            imageStatus = IMAGE_FAILED;
            message = "图片本地化失败：" + failed + "/" + total;
        }
        try {
            NotionArticleSyncLog update = new NotionArticleSyncLog();
            update.setId(logId);
            update.setImageStatus(imageStatus);
            update.setChangedFields(result.getChangedCount());
            update.setTotalImages(total);
            update.setLocalizedImages(localized);
            update.setFailedImages(failed);
            update.setMessage(message);
            update.setWarnings(joinWarnings(result.getWarnings()));
            notionArticleSyncLogMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("Failed to update Notion image result log, logId={}", logId, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markImageFailed(Long logId, Exception error) {
        updateImageStatus(logId, IMAGE_FAILED, shortText(error == null ? "图片本地化失败" : error.getMessage(), 500), error);
    }

    public List<NotionArticleSyncLog> listByArticleId(Long articleId) {
        if (articleId == null || articleId <= 0) {
            return Collections.emptyList();
        }
        return notionArticleSyncLogMapper.selectList(new LambdaQueryWrapper<NotionArticleSyncLog>()
                .eq(NotionArticleSyncLog::getArticleId, articleId)
                .orderByDesc(NotionArticleSyncLog::getId)
                .last("limit 30"));
    }

    public List<NotionArticleSyncLog> listRecent() {
        return notionArticleSyncLogMapper.selectList(new LambdaQueryWrapper<NotionArticleSyncLog>()
                .orderByDesc(NotionArticleSyncLog::getId)
                .last("limit 50"));
    }

    public NotionArticleSyncLog findLastSuccessfulSync(Long articleId) {
        if (articleId == null || articleId <= 0) {
            return null;
        }
        return notionArticleSyncLogMapper.selectOne(new LambdaQueryWrapper<NotionArticleSyncLog>()
                .select(NotionArticleSyncLog::getId, NotionArticleSyncLog::getCreateTime, NotionArticleSyncLog::getUpdateTime)
                .eq(NotionArticleSyncLog::getArticleId, articleId)
                .eq(NotionArticleSyncLog::getStatus, STATUS_SUCCESS)
                .orderByDesc(NotionArticleSyncLog::getId)
                .last("limit 1"));
    }

    public boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return notionArticleSyncLogMapper.deleteBatchIds(ids) > 0;
    }

    private void updateImageStatus(Long logId, String imageStatus, String message, Exception error) {
        if (logId == null) {
            return;
        }
        try {
            NotionArticleSyncLog update = new NotionArticleSyncLog();
            update.setId(logId);
            update.setImageStatus(imageStatus);
            update.setMessage(shortText(message, 500));
            if (error != null) {
                update.setErrorDetail(shortText(stackTrace(error), 4000));
            }
            notionArticleSyncLogMapper.updateById(update);
        } catch (Exception ex) {
            log.warn("Failed to update Notion image status log, logId={}", logId, ex);
        }
    }

    private String joinWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }
        return shortText(String.join("\n", warnings), 2000);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String shortText(String value, int maxLength) {
        String text = StringUtils.defaultString(value).trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String stackTrace(Exception error) {
        if (error == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
