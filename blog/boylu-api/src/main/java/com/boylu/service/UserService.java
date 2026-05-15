package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysFeedback;
import com.boylu.entity.SysUser;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.comment.CommentListVo;

import java.util.List;

/**
 * @author: boylu
 * @date: 2025/1/11
 * @description:
 */
public interface UserService {

    /**
     * 查询我的评论
     * @return
     */
    IPage<CommentListVo> selectMyComment();

    /**
     * 删除我的评论
     * @param ids
     * @return
     */
    Void delMyComment(List<Long> ids);

    /**
     * 查询我的点赞
     * @return
     */
    IPage<ArticleListVo> selectMyLike();

    /**
     * 查询我的回复
     * @return
     */
    IPage<CommentListVo> getMyReply();

    /**
     * 修改我的资料
     * @param user
     */
    void updateProfile(SysUser user);

    /**
     * 查询我的文章
     * @return
     */
    IPage<ArticleListVo> selectMyArticle(SysArticle article);

}
