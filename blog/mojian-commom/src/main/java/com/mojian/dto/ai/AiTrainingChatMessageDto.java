package com.mojian.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI training chat message")
public class AiTrainingChatMessageDto {

    @ApiModelProperty(value = "Message role, supports user/assistant")
    private String role;

    @ApiModelProperty(value = "Message content")
    private String content;
}
