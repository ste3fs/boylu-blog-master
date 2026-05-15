package com.boylu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.mapper.SysMomentMapper;
import com.boylu.service.MomentService;
import com.boylu.utils.PageUtil;
import com.boylu.vo.moment.MomentPageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: boylu
 * @date: 2025/2/5
 * @description:
 */
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final SysMomentMapper baseMapper;

    @Override
    public IPage<MomentPageVo> getMomentList() {
        return baseMapper.selectPage(PageUtil.getPage());
    }
}
