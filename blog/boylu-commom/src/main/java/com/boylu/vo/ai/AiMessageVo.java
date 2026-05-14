package com.boylu.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AI 消息视图对象")
public class AiMessageVo {

    @ApiModelProperty(value = "消息id")
    private Long id;

    @ApiModelProperty(value = "会话id")
    private Long sessionId;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "内容")
    private String content;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "来源引用")
    private String sourceRef;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createTime;
}
