package com.boylu.dto.feedback;

import com.boylu.entity.SysFeedback;
import lombok.Data;

/**
 * @author: boylu
 * @date: 2025/1/12
 * @description:
 */
@Data
public class SysFeedbackQueryDto extends SysFeedback {

    private String source;
}
