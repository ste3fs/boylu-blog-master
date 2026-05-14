package com.boylu.vo.moment;

import com.boylu.entity.SysMoment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: boylu
 * @date: 2025/2/5
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MomentPageVo extends SysMoment {

    private String nickname;

    private String avatar;

}
