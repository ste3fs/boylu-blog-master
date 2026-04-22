package com.mojian.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI admin config view")
public class AiAdminConfigVo {

    @ApiModelProperty(value = "Persona prompt path")
    private String personaPromptPath;

    @ApiModelProperty(value = "Persona prompt content")
    private String personaPrompt;

    @ApiModelProperty(value = "AI provider base url")
    private String baseUrl;

    @ApiModelProperty(value = "AI provider api key masked")
    private String apiKeyMasked;

    @ApiModelProperty(value = "AI model")
    private String model;

    @ApiModelProperty(value = "Training prompt")
    private String trainingPrompt;

    @ApiModelProperty(value = "Training enabled")
    private Boolean trainingEnabled;
}
