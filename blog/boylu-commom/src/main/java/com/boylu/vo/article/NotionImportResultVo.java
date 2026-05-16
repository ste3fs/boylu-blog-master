package com.boylu.vo.article;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotionImportResultVo {

    private Long articleId;

    private String title;

    private Integer importedBlocks;

    private List<String> warnings;
}
