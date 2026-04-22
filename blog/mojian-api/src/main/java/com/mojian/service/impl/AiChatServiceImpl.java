package com.mojian.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mojian.dto.ai.AiChatSendDto;
import com.mojian.dto.ai.AiSessionCreateDto;
import com.mojian.entity.AiMessage;
import com.mojian.entity.AiSession;
import com.mojian.exception.ServiceException;
import com.mojian.mapper.AiMessageMapper;
import com.mojian.mapper.AiSessionMapper;
import com.mojian.service.AiChatService;
import com.mojian.utils.AiUtil;
import com.mojian.utils.PageUtil;
import com.mojian.vo.ai.AiChatReplyVo;
import com.mojian.vo.ai.AiMessageVo;
import com.mojian.vo.ai.AiSessionDetailVo;
import com.mojian.vo.ai.AiSessionListVo;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String DEFAULT_SESSION_TITLE = "新对话";
    private static final String DEFAULT_PERSONA_RESOURCE = "ai/persona-boylu.txt";

    private final AiSessionMapper aiSessionMapper;
    private final AiMessageMapper aiMessageMapper;
    private final AiUtil aiUtil;

    @Value("${ai.personaPromptPath:}")
    private String personaPromptPath;

    @Override
    public IPage<AiSessionListVo> getSessionList() {
        return aiSessionMapper.selectSessionPage(PageUtil.getPage(), StpUtil.getLoginIdAsLong());
    }

    @Override
    public AiSessionDetailVo getSessionDetail(Long sessionId) {
        AiSession session = getOwnedSession(sessionId);
        AiSessionDetailVo detailVo = new AiSessionDetailVo();
        detailVo.setId(session.getId());
        detailVo.setTitle(session.getTitle());
        detailVo.setModel(session.getModel());
        detailVo.setMessages(aiMessageMapper.selectMessagesBySessionId(sessionId));
        return detailVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSessionDetailVo createSession(AiSessionCreateDto dto) {
        AiSession session = AiSession.builder()
                .userId(StpUtil.getLoginIdAsLong())
                .title(resolveSessionTitle(dto == null ? null : dto.getTitle()))
                .model(aiUtil.getModel())
                .lastMessageAt(LocalDateTime.now())
                .build();
        aiSessionMapper.insert(session);
        return getSessionDetail(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        AiSession session = getOwnedSession(sessionId);
        aiMessageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, session.getId()));
        aiSessionMapper.deleteById(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatReplyVo sendMessage(AiChatSendDto dto) {
        PreparedChat prepared = prepareChat(dto);
        String assistantContent = StringUtils.trimToEmpty(aiUtil.send(buildModelMessages(prepared.getSession().getId(), prepared.getUserContent())));
        if (StringUtils.isBlank(assistantContent)) {
            throw new ServiceException("AI 暂时没有返回内容，请稍后再试");
        }

        AiMessage assistantMessage = saveAssistantMessage(prepared.getSession(), assistantContent);
        updateSessionAfterReply(prepared.getSession(), prepared.getUserContent());

        AiChatReplyVo replyVo = new AiChatReplyVo();
        replyVo.setSessionId(prepared.getSession().getId());
        replyVo.setTitle(prepared.getSession().getTitle());
        replyVo.setUserMessage(toMessageVo(prepared.getUserMessage()));
        replyVo.setAssistantMessage(toMessageVo(assistantMessage));
        return replyVo;
    }

    @Override
    public SseEmitter streamMessage(AiChatSendDto dto) {
        PreparedChat prepared = prepareChat(dto);
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "session", buildSessionPayload(prepared.getSession()));
                sendEvent(emitter, "user", toMessageVo(prepared.getUserMessage()));

                String assistantContent = StringUtils.trimToEmpty(aiUtil.send(buildModelMessages(prepared.getSession().getId(), prepared.getUserContent())));
                if (StringUtils.isBlank(assistantContent)) {
                    sendEvent(emitter, "error", Collections.singletonMap("message", "AI 暂时没有返回内容，请稍后再试"));
                    emitter.complete();
                    return;
                }

                sendEvent(emitter, "delta", Collections.singletonMap("content", assistantContent));
                AiMessage assistantMessage = saveAssistantMessage(prepared.getSession(), assistantContent);
                updateSessionAfterReply(prepared.getSession(), prepared.getUserContent());
                sendEvent(emitter, "done", toMessageVo(assistantMessage));
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendEvent(emitter, "error", Collections.singletonMap("message", "AI 请求失败，请稍后再试"));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    @Transactional(rollbackFor = Exception.class)
    protected PreparedChat prepareChat(AiChatSendDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getContent())) {
            throw new ServiceException("发送内容不能为空");
        }

        String userContent = normalizeContent(dto.getContent());
        if (StringUtils.isBlank(userContent)) {
            throw new ServiceException("发送内容不能为空");
        }

        AiSession session = resolveOrCreateSession(dto.getSessionId(), userContent);
        AiMessage userMessage = AiMessage.builder()
                .sessionId(session.getId())
                .role(ROLE_USER)
                .content(userContent)
                .build();
        aiMessageMapper.insert(userMessage);
        return new PreparedChat(session, userMessage, userContent);
    }

    private List<ChatMessage> buildModelMessages(Long sessionId, String userContent) {
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = loadPersonaPrompt();
        if (StringUtils.isNotBlank(systemPrompt)) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build());
        }

        List<AiMessageVo> history = aiMessageMapper.selectRecentMessages(sessionId, 12);
        if (history != null) {
            history.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> StringUtils.isNotBlank(item.getContent()))
                    .forEach(item -> {
                        ChatMessageRole role = ROLE_ASSISTANT.equals(item.getRole()) ? ChatMessageRole.ASSISTANT : ChatMessageRole.USER;
                        messages.add(ChatMessage.builder()
                                .role(role)
                                .content(item.getContent())
                                .build());
                    });
        }

        if (messages.isEmpty()) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(userContent)
                    .build());
        }
        return messages;
    }

    private String loadPersonaPrompt() {
        try {
            if (StringUtils.isNotBlank(personaPromptPath) && FileUtil.exist(personaPromptPath)) {
                return FileUtil.readString(personaPromptPath, StandardCharsets.UTF_8);
            }
            ClassPathResource resource = new ClassPathResource(DEFAULT_PERSONA_RESOURCE);
            if (!resource.exists()) {
                return "";
            }
            try (InputStream inputStream = resource.getInputStream()) {
                return IoUtil.read(inputStream, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    private AiSession resolveOrCreateSession(Long sessionId, String firstQuestion) {
        if (sessionId != null) {
            return getOwnedSession(sessionId);
        }
        AiSession session = AiSession.builder()
                .userId(StpUtil.getLoginIdAsLong())
                .title(resolveSessionTitle(firstQuestion))
                .model(aiUtil.getModel())
                .lastMessageAt(LocalDateTime.now())
                .build();
        aiSessionMapper.insert(session);
        return session;
    }

    private AiSession getOwnedSession(Long sessionId) {
        AiSession session = aiSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("会话不存在或已被删除");
        }
        return session;
    }

    private void updateSessionAfterReply(AiSession session, String userContent) {
        AiSession update = new AiSession();
        update.setId(session.getId());
        update.setLastMessageAt(LocalDateTime.now());
        if (StringUtils.equals(session.getTitle(), DEFAULT_SESSION_TITLE) && StringUtils.isNotBlank(userContent)) {
            update.setTitle(resolveSessionTitle(userContent));
            session.setTitle(update.getTitle());
        }
        aiSessionMapper.updateById(update);
    }

    private AiMessage saveAssistantMessage(AiSession session, String assistantContent) {
        AiMessage assistantMessage = AiMessage.builder()
                .sessionId(session.getId())
                .role(ROLE_ASSISTANT)
                .content(assistantContent)
                .build();
        aiMessageMapper.insert(assistantMessage);
        return assistantMessage;
    }

    private AiMessageVo toMessageVo(AiMessage message) {
        AiMessageVo vo = new AiMessageVo();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSourceType(message.getSourceType());
        vo.setSourceRef(message.getSourceRef());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object payload) throws Exception {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(payload, MediaType.APPLICATION_JSON));
    }

    private LinkedHashMap<String, Object> buildSessionPayload(AiSession session) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.getId());
        payload.put("title", session.getTitle());
        return payload;
    }

    private String resolveSessionTitle(String raw) {
        String normalized = normalizeContent(raw);
        if (StringUtils.isBlank(normalized)) {
            return DEFAULT_SESSION_TITLE;
        }
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private String normalizeContent(String raw) {
        return StringUtils.normalizeSpace(StringUtils.defaultString(raw)).trim();
    }

    @RequiredArgsConstructor
    private static class PreparedChat {
        private final AiSession session;
        private final AiMessage userMessage;
        private final String userContent;

        public AiSession getSession() {
            return session;
        }

        public AiMessage getUserMessage() {
            return userMessage;
        }

        public String getUserContent() {
            return userContent;
        }
    }
}
