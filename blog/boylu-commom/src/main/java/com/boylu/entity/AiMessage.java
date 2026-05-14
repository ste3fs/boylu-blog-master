package com.boylu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_message")
@ApiModel(value = "AI 消息对象")
public class AiMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "消息id")
    private Long id;

    @ApiModelProperty(value = "会话id")
    private Long sessionId;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "内容")
    private String content;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "来源引用")
    private String sourceRef;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createTime;
}
