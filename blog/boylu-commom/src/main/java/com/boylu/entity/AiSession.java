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
@TableName("ai_session")
@ApiModel(value = "AI 会话对象")
public class AiSession implements Serializable {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "会话id")
    private Long id;

    @ApiModelProperty(value = "用户id")
    private Long userId;

    @ApiModelProperty(value = "会话标题")
    private String title;

    @ApiModelProperty(value = "模型")
    private String model;

    @ApiModelProperty(value = "人设版本")
    private String personaVersion;

    @ApiModelProperty(value = "最近消息时间")
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime lastMessageAt;

    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime updateTime;
}
