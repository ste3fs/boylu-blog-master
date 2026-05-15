package com.boylu.vo.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "文章封面图片元数据")
public class CoverImageVo implements Serializable {

    @ApiModelProperty(value = "图片资源唯一 key")
    private String key;

    @ApiModelProperty(value = "图片替代文本")
    private String alt;

    @ApiModelProperty(value = "原始宽度")
    private Integer width;

    @ApiModelProperty(value = "原始高度")
    private Integer height;

    @ApiModelProperty(value = "主色")
    private String dominantColor;

    @ApiModelProperty(value = "模糊占位图 data url")
    private String blurDataURL;

    @ApiModelProperty(value = "多格式多尺寸图片地址")
    private Map<String, Map<Integer, String>> variants;

    @ApiModelProperty(value = "兜底图片地址")
    private String fallback;

    @ApiModelProperty(value = "内容 hash")
    private String hash;
}
