package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.entity.SysNotifications;
import com.boylu.service.CommentService;
import com.boylu.utils.NotificationsUtil;
import com.boylu.utils.SensitiveUtil;
import com.boylu.vo.comment.CommentListVo;
import com.boylu.entity.SysComment;
import com.boylu.mapper.SysCommentMapper;
import com.boylu.utils.IpUtil;
import com.boylu.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final SysCommentMapper sysCommentMapper;

    private final NotificationsUtil notificationsUtil;

    @Override
    public IPage<CommentListVo> getComments(Integer articleId,String sortType) {
        IPage<CommentListVo> page = sysCommentMapper.getComments(PageUtil.getPage(),articleId,sortType);
        //获取所有子评论
        page.getRecords().forEach(commentListVo -> {
            List<CommentListVo> children = sysCommentMapper.getChildrenComment(commentListVo.getId());
            commentListVo.setChildren(children);
        });
        return page;
    }

    @Override
    public void add(SysComment sysComment) {

        String ip = IpUtil.getIp();
        sysComment.setIp(ip);
        sysComment.setIpSource(IpUtil.getIp2region(ip));
        sysComment.setUserId(StpUtil.getLoginIdAsLong());
        sysComment.setContent(SensitiveUtil.filter(sysComment.getContent()));

        sysCommentMapper.insert(sysComment);

        ThreadUtil.execAsync(() -> {
            //发送通知事件
            SysNotifications notifications = SysNotifications.builder()
                    .title(sysComment.getReplyUserId() != null ? "评论回复通知" : "新评论通知")
                    .message(sysComment.getContent())
                    .articleId(sysComment.getArticleId())
                    .isRead(0)
                    .type("comment")
                    .userId(sysComment.getReplyUserId())
                    .fromUserId(sysComment.getUserId())
                    .build();
            notificationsUtil.publish(notifications);
        });
    }
}
