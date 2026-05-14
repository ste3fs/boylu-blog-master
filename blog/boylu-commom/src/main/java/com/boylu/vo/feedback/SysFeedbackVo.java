package com.boylu.vo.feedback;

import com.boylu.entity.SysFeedback;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: boylu
 * @date: 2025/1/12
 * @description:
 */
@Data
@ApiModel(value = "反馈对象vo")
public class SysFeedbackVo extends SysFeedback {

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "头像")
    private String avatar;
}
