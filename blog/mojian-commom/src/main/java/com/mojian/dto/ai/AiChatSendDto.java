package com.mojian.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AI 发送消息参数")
public class AiChatSendDto {

    @ApiModelProperty(value = "会话id")
    private Long sessionId;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "模式：chat|reason|site_search|site_deep")
    private String mode;

    @ApiModelProperty(value = "是否开启站内检索")
    private Boolean searchEnabled;

    @ApiModelProperty(value = "是否开启深度研究")
    private Boolean deepResearchEnabled;

    @ApiModelProperty(value = "是否开启推理模式")
    private Boolean reasonEnabled;

    @ApiModelProperty(value = "附件列表")
    private List<AiAttachmentDto> attachments;
}
