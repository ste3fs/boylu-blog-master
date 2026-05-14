package com.boylu.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI RAG 诊断参数")
public class AiRagDiagnosticsDto {

    @ApiModelProperty(value = "诊断问题")
    private String content;

    @ApiModelProperty(value = "是否开启站内搜索")
    private Boolean searchEnabled;

    @ApiModelProperty(value = "是否开启深研")
    private Boolean deepResearchEnabled;
}
