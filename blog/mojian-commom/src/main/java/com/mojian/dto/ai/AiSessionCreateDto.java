package com.mojian.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "创建 AI 会话参数")
public class AiSessionCreateDto {

    @ApiModelProperty(value = "会话标题")
    private String title;
}
