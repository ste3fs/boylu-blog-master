package com.mojian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mojian.dto.ai.AiChatSendDto;
import com.mojian.dto.ai.AiSessionCreateDto;
import com.mojian.vo.ai.AiChatReplyVo;
import com.mojian.vo.ai.AiSessionDetailVo;
import com.mojian.vo.ai.AiSessionListVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    IPage<AiSessionListVo> getSessionList();

    AiSessionDetailVo getSessionDetail(Long sessionId);

    AiSessionDetailVo createSession(AiSessionCreateDto dto);

    void deleteSession(Long sessionId);

    AiChatReplyVo sendMessage(AiChatSendDto dto);

    SseEmitter streamMessage(AiChatSendDto dto);
}
