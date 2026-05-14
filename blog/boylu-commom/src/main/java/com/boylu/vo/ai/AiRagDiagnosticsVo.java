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
public class AiRagDiagnosticsVo {

    private String question;

    private Boolean searchEnabled;

    private Boolean deepResearchEnabled;

    private List<String> keywords = new ArrayList<>();

    private List<String> effectiveKeywords = new ArrayList<>();

    private List<String> fallbackKeywords = new ArrayList<>();

    private Boolean fallbackUsed;

    private Boolean siteContextUsed;

    private String siteContextPreview;

    private String promptPreview;

    private Long costMs;

    private List<ArticleHitItem> articleHits = new ArrayList<>();

    private List<ChunkHitItem> chunkHits = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleHitItem {
        private Long id;
        private String title;
        private String summary;

        @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
        private LocalDateTime updateTime;

        private Boolean selected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkHitItem {
        private Long articleId;
        private String articleTitle;
        private Integer chunkIndex;
        private Integer score;
        private String excerpt;
    }
}
