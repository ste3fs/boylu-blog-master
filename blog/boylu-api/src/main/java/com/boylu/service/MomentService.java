package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.vo.moment.MomentPageVo;

/**
 * @author: boylu
 * @date: 2025/2/5
 * @description:
 */
public interface MomentService {
    IPage<MomentPageVo> getMomentList();

}
