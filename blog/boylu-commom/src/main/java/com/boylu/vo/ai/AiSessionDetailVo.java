package com.boylu.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AI 会话详情视图对象")
public class AiSessionDetailVo {

    @ApiModelProperty(value = "会话id")
    private Long id;

    @ApiModelProperty(value = "会话标题")
    private String title;

    @ApiModelProperty(value = "模型")
    private String model;

    @ApiModelProperty(value = "消息列表")
    private List<AiMessageVo> messages;
}
