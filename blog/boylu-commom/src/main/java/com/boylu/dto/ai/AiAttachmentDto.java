package com.boylu.dto.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AI 附件参数")
public class AiAttachmentDto {

    @ApiModelProperty(value = "附件名称")
    private String name;

    @ApiModelProperty(value = "附件地址")
    private String url;

    @ApiModelProperty(value = "文件类型")
    private String contentType;

    @ApiModelProperty(value = "文件大小")
    private Long size;

    @ApiModelProperty(value = "文本摘要")
    private String excerpt;
}
