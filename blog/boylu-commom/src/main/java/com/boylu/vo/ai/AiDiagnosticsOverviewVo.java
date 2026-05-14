package com.boylu.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiDiagnosticsOverviewVo {

    private List<StatusSummaryItem> attachmentStatusSummary = new ArrayList<>();

    private List<ProviderCallItem> recentProviderCalls = new ArrayList<>();

    private List<AttachmentParseItem> recentAttachmentParses = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusSummaryItem {
        private String status;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderCallItem {
        private String providerType;
        private String model;
        private String requestType;
        private Integer success;
        private Long latencyMs;
        private String errorMessage;

        @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
        private LocalDateTime createTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentParseItem {
        private String fileId;
        private String fileName;
        private String contentType;
        private String parseStatus;
        private String parsedExcerpt;
        private String errorMessage;

        @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
        private LocalDateTime updateTime;
    }
}
