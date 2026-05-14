package com.boylu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boylu.entity.SysMessage;
import com.boylu.mapper.SysMessageMapper;
import com.boylu.service.SysMessageService;
import com.boylu.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: boylu
 * @date: 2025/1/2
 * @description:
 */
@Service
@RequiredArgsConstructor
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements SysMessageService {

    @Override
    public Page<SysMessage> selectList() {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>().orderByDesc(SysMessage::getCreateTime);
        return baseMapper.selectPage(PageUtil.getPage(),wrapper);
    }
}
