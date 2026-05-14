package com.boylu.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(value = "AI training chat request")
public class AiTrainingChatDto {

    @ApiModelProperty(value = "Chat transcript")
    private List<AiTrainingChatMessageDto> messages = new ArrayList<>();

    @ApiModelProperty(value = "Current extracted draft prompt")
    private String currentDraftPrompt;
}
