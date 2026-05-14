package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysComment;
import com.boylu.entity.SysUser;
import com.boylu.mapper.*;
import com.boylu.service.UserService;
import com.boylu.utils.PageUtil;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.comment.CommentListVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: boylu
 * @date: 2025/1/11
 * @description:
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final SysCommentMapper commentMapper;

    private final SysArticleMapper articleMapper;

    private final SysTagMapper tagMapper;

    @Override
    public IPage<CommentListVo> selectMyComment() {
        return commentMapper.selectMyComment(PageUtil.getPage(), StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void delMyComment(List<Long> ids) {
        commentMapper.deleteBatchIds(ids);
        commentMapper.delete(new LambdaQueryWrapper<SysComment>()
                .in(SysComment::getParentId, ids));
        return null;
    }

    @Override
    public IPage<ArticleListVo> selectMyLike() {
        return articleMapper.selectMyLike(PageUtil.getPage(),StpUtil.getLoginIdAsLong());
    }

    @Override
    public IPage<CommentListVo> getMyReply() {
        return commentMapper.getMyReply(PageUtil.getPage(),StpUtil.getLoginIdAsLong());
    }

    @Override
    public void updateProfile(SysUser user) {
        sysUserMapper.updateById(user);
    }

    @Override
    public IPage<ArticleListVo> selectMyArticle(SysArticle article) {
        article.setUserId(StpUtil.getLoginIdAsLong());
        return articleMapper.selectMyArticle(PageUtil.getPage(),article);
    }

}
