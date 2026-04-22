package com.mojian.vo.dashboard;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitTrendData {

    @ApiModelProperty(value = "日期")
    private String date;

    @ApiModelProperty(value = "展示标签")
    private String label;

    @ApiModelProperty(value = "唯一访客数")
    private Long visitCount;

    @ApiModelProperty(value = "浏览量")
    private Long viewCount;
}
