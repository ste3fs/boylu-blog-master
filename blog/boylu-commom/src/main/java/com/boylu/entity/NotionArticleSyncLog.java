package com.boylu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("notion_article_sync_log")
public class NotionArticleSyncLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String articleTitle;

    private String sourceUrl;

    private String action;

    private String status;

    private String imageStatus;

    private Integer importedBlocks;

    private Integer changedFields;

    private Integer totalImages;

    private Integer localizedImages;

    private Integer failedImages;

    private String message;

    private String warnings;

    private String errorDetail;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime updateTime;
}
