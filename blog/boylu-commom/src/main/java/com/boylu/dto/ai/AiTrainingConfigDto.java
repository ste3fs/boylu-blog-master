package com.boylu.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI training config")
public class AiTrainingConfigDto {

    @ApiModelProperty(value = "Training prompt")
    private String trainingPrompt;

    @ApiModelProperty(value = "Training enabled")
    private Boolean trainingEnabled;
}
