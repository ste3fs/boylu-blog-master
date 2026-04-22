package com.mojian.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI runtime config")
public class AiRuntimeConfigDto {

    @ApiModelProperty(value = "AI provider base url")
    private String baseUrl;

    @ApiModelProperty(value = "AI provider api key")
    private String apiKey;

    @ApiModelProperty(value = "AI model")
    private String model;
}
