package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.vo.comment.CommentListVo;
import com.boylu.entity.SysComment;

public interface CommentService {

    /**
     * 获取评论列表
     * @param articleId
     * @return
     */

    IPage<CommentListVo> getComments(Integer articleId,String sortType);

    /**
     * 新增评论
     * @param sysComment
     * @return
     */
    void add(SysComment sysComment);
}
