package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.entity.SysArticle;
import com.boylu.entity.SysComment;
import com.boylu.entity.SysUser;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.*;
import com.boylu.service.UserService;
import com.boylu.utils.PageUtil;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.comment.CommentListVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: boylu
 * @date: 2025/1/11
 * @description:
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

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
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        Long loginUserId = StpUtil.getLoginIdAsLong();
        List<Integer> ownedCommentIds = commentMapper.selectList(new LambdaQueryWrapper<SysComment>()
                        .select(SysComment::getId)
                        .in(SysComment::getId, ids)
                        .eq(SysComment::getUserId, loginUserId))
                .stream()
                .map(SysComment::getId)
                .collect(Collectors.toList());

        if (ownedCommentIds.isEmpty()) {
            throw new ServiceException("没有可删除的评论");
        }

        commentMapper.delete(new LambdaQueryWrapper<SysComment>()
                .in(SysComment::getId, ownedCommentIds));
        commentMapper.delete(new LambdaQueryWrapper<SysComment>()
                .in(SysComment::getParentId, ownedCommentIds));
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
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(SysUser user) {
        if (user == null) {
            throw new ServiceException("用户信息不能为空");
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, StpUtil.getLoginIdAsInt());
        boolean hasUpdate = false;

        if (user.getNickname() != null) {
            String nickname = StringUtils.trim(user.getNickname());
            if (StringUtils.isBlank(nickname) || nickname.length() > 20) {
                throw new ServiceException("昵称不能为空且不能超过20个字符");
            }
            updateWrapper.set(SysUser::getNickname, nickname);
            hasUpdate = true;
        }

        if (user.getEmail() != null) {
            String email = StringUtils.trim(user.getEmail());
            if (StringUtils.isBlank(email) || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new ServiceException("邮箱格式不正确");
            }
            updateWrapper.set(SysUser::getEmail, email);
            hasUpdate = true;
        }

        if (user.getSignature() != null) {
            updateWrapper.set(SysUser::getSignature, StringUtils.trimToEmpty(user.getSignature()));
            hasUpdate = true;
        }

        if (user.getSex() != null) {
            Integer sex = user.getSex();
            if (sex != 0 && sex != 1 && sex != 2) {
                throw new ServiceException("性别参数不正确");
            }
            updateWrapper.set(SysUser::getSex, sex);
            hasUpdate = true;
        }

        if (user.getAvatar() != null) {
            String avatar = StringUtils.trim(user.getAvatar());
            if (StringUtils.isNotBlank(avatar)) {
                updateWrapper.set(SysUser::getAvatar, avatar);
                hasUpdate = true;
            }
        }

        if (hasUpdate) {
            sysUserMapper.update(null, updateWrapper);
        }
    }

    @Override
    public IPage<ArticleListVo> selectMyArticle(SysArticle article) {
        article.setUserId(StpUtil.getLoginIdAsLong());
        return articleMapper.selectMyArticle(PageUtil.getPage(),article);
    }

}
