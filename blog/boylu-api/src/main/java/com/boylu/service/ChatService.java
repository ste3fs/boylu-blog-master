package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.vo.chat.ChatSendMsgVo;

public interface ChatService {

    /**
     * 获取消息列表
     * @return
     */
    IPage<ChatSendMsgVo> getChatMsgList();

    /**
     * 发送消息
     * @param chatSendMsgVo
     */
    void sendMsg(ChatSendMsgVo chatSendMsgVo);

    /**
     * 撤回消息
     * @param chatSendMsgVo
     */
    void recallMsg(ChatSendMsgVo chatSendMsgVo);
}
