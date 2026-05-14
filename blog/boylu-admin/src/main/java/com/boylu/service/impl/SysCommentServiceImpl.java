package com.boylu.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boylu.entity.SysComment;
import com.boylu.mapper.SysCommentMapper;
import com.boylu.service.SysCommentService;
import com.boylu.utils.PageUtil;
import com.boylu.vo.comment.SysCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: boylu
 * @date: 2025/1/2
 * @description:
 */
@Service
@RequiredArgsConstructor
public class SysCommentServiceImpl extends ServiceImpl<SysCommentMapper,SysComment> implements SysCommentService {

    @Override
    public Page<SysCommentVO> selectList() {
        return baseMapper.selectPage(PageUtil.getPage());
    }
}
