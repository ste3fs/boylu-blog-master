package com.boylu.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.boylu.utils.DateUtil;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OauthBindingVo {

    private String source;

    private String username;

    private String nickname;

    private String avatar;

    private Boolean bindable;

    @JsonFormat(pattern = DateUtil.YYYY_MM_DD_HH_MM_SS, timezone = "GMT+8")
    private LocalDateTime bindTime;
}
