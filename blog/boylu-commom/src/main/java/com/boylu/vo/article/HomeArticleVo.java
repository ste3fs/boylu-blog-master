package com.boylu.vo.article;

import com.boylu.utils.DateUtil;
import com.boylu.vo.tag.TagListVo;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "首页文章轻量列表视图")
public class HomeArticleVo implements Serializable {

    @ApiModelProperty(value = "文章 id")
    private Long id;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "文章 slug")
    private String slug;

    @ApiModelProperty(value = "摘要")
    private String excerpt;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "分类")
    private String category;

    @ApiModelProperty(value = "标签")
    private List<TagListVo> tags;

    @ApiModelProperty(value = "封面图片元数据")
    private CoverImageVo coverImage;

    @ApiModelProperty(value = "预计阅读分钟")
    private Integer readingTime;

    @ApiModelProperty(value = "浏览量")
    private Integer views;

    @JsonIgnore
    private String coverImageJson;

    @JsonIgnore
    private String legacyCover;
}
