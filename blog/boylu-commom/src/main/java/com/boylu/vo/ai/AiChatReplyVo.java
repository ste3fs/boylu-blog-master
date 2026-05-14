package com.boylu.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI 聊天回复视图对象")
public class AiChatReplyVo {

    @ApiModelProperty(value = "会话id")
    private Long sessionId;

    @ApiModelProperty(value = "会话标题")
    private String title;

    @ApiModelProperty(value = "用户消息")
    private AiMessageVo userMessage;

    @ApiModelProperty(value = "AI 回复")
    private AiMessageVo assistantMessage;
}
