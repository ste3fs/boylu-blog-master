package com.boylu.dto.article;

import lombok.Data;

import java.util.List;

@Data
public class NotionImportDto {

    private String pageUrl;

    private String categoryName;

    private List<String> tags;

    private String cover;

    private String summary;

    private String keywords;

    private Integer readType;

    private Integer status;

    private Integer isOriginal;

    private Integer isStick;

    private Integer isCarousel;

    private Integer isRecommend;

    private Boolean importImages;
}
