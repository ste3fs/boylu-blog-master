package com.mojian.controller.ai;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mojian.common.Result;
import com.mojian.dto.ai.AiChatSendDto;
import com.mojian.dto.ai.AiSessionCreateDto;
import com.mojian.service.AiChatService;
import com.mojian.vo.ai.AiChatReplyVo;
import com.mojian.vo.ai.AiSessionDetailVo;
import com.mojian.vo.ai.AiSessionListVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SaCheckLogin
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Api(tags = "门户-AI 助手")
public class AiChatController {

    private final AiChatService aiChatService;

    @GetMapping("/sessions")
    @ApiOperation(value = "获取 AI 会话列表")
    public Result<IPage<AiSessionListVo>> getSessionList() {
        return Result.success(aiChatService.getSessionList());
    }

    @GetMapping("/session/{sessionId}")
    @ApiOperation(value = "获取 AI 会话详情")
    public Result<AiSessionDetailVo> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(aiChatService.getSessionDetail(sessionId));
    }

    @PostMapping("/session")
    @ApiOperation(value = "创建 AI 会话")
    public Result<AiSessionDetailVo> createSession(@RequestBody AiSessionCreateDto dto) {
        return Result.success(aiChatService.createSession(dto));
    }

    @DeleteMapping("/session/{sessionId}")
    @ApiOperation(value = "删除 AI 会话")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        aiChatService.deleteSession(sessionId);
        return Result.success();
    }

    @PostMapping("/chat/send")
    @ApiOperation(value = "发送 AI 消息")
    public Result<AiChatReplyVo> sendMessage(@RequestBody AiChatSendDto dto) {
        return Result.success(aiChatService.sendMessage(dto));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "流式发送 AI 消息")
    public SseEmitter streamMessage(@RequestBody AiChatSendDto dto) {
        return aiChatService.streamMessage(dto);
    }
}
