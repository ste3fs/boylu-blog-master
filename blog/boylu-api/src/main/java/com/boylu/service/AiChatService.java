package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.dto.ai.AiChatSendDto;
import com.boylu.dto.ai.AiSessionCreateDto;
import com.boylu.vo.ai.AiChatReplyVo;
import com.boylu.vo.ai.AiSessionDetailVo;
import com.boylu.vo.ai.AiSessionListVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    IPage<AiSessionListVo> getSessionList();

    AiSessionDetailVo getSessionDetail(Long sessionId);

    AiSessionDetailVo createSession(AiSessionCreateDto dto);

    void deleteSession(Long sessionId);

    AiChatReplyVo sendMessage(AiChatSendDto dto);

    SseEmitter streamMessage(AiChatSendDto dto);
}
