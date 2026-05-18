package com.boylu.service.notion;

import com.boylu.service.SysArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionImageLocalizationQueueJob {

    private final SysArticleService sysArticleService;

    @Value("${notion.image-localize-queue-enabled:true}")
    private boolean queueEnabled;

    @Value("${notion.image-localize-queue-batch-size:5}")
    private int queueBatchSize;

    @Scheduled(fixedDelayString = "${notion.image-localize-queue-fixed-delay-ms:15000}")
    public void processQueue() {
        if (!queueEnabled) {
            return;
        }
        try {
            int processed = sysArticleService.processPendingNotionImageLocalizationQueue(queueBatchSize);
            if (processed > 0) {
                log.info("Processed Notion image localization queue, dispatched={}", processed);
            }
        } catch (Exception ex) {
            log.warn("Process Notion image localization queue failed", ex);
        }
    }
}
