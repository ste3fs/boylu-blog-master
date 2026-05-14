package com.boylu.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AI 会话列表视图对象")
public class AiSessionListVo {

    @ApiModelProperty(value = "会话id")
    private Long id;

    @ApiModelProperty(value = "会话标题")
    private String title;

    @ApiModelProperty(value = "模型")
    private String model;

    @ApiModelProperty(value = "最近消息时间")
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime lastMessageAt;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createTime;
}
