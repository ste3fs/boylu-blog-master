package com.mojian.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(value = "AI training chat response")
public class AiTrainingChatVo {

    @ApiModelProperty(value = "Assistant reply")
    private String assistantReply;

    @ApiModelProperty(value = "Extracted draft prompt")
    private String draftPrompt;

    @ApiModelProperty(value = "Current extraction summary")
    private String summary;

    @ApiModelProperty(value = "Extracted highlights")
    private List<String> highlights = new ArrayList<>();

    @ApiModelProperty(value = "Boundary warnings")
    private List<String> warnings = new ArrayList<>();

    @ApiModelProperty(value = "Missing dimensions to continue asking")
    private List<String> missingDimensions = new ArrayList<>();

    @ApiModelProperty(value = "Whether the current transcript is enough to apply")
    private Boolean readyToApply;
}
