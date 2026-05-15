package com.boylu.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boylu.entity.SysComment;
import com.boylu.vo.comment.SysCommentVO;

/**
 * @author: boylu
 * @date: 2025/1/2
 * @description:
 */
public interface SysCommentService extends IService<SysComment> {

    /**
     * 获取评论列表
     * @return
     */
    Page<SysCommentVO> selectList();



}
