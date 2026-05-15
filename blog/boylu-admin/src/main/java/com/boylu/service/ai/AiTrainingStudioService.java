package com.boylu.service.ai;

import com.boylu.dto.ai.AiTrainingChatDto;
import com.boylu.dto.ai.AiTrainingChatMessageDto;
import com.boylu.exception.ServiceException;
import com.boylu.utils.AiUtil;
import com.boylu.vo.ai.AiTrainingChatVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTrainingStudioService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final int MAX_MESSAGE_COUNT = 18;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final AiUtil aiUtil;

    public AiTrainingChatVo chat(AiTrainingChatDto dto) {
        List<TrainingMessage> transcript = normalizeMessages(dto == null ? null : dto.getMessages());
        String assistantReply = buildAssistantReply(transcript);

        AiTrainingChatVo vo = new AiTrainingChatVo();
        vo.setAssistantReply(assistantReply);

        if (transcript.stream().noneMatch(item -> ROLE_USER.equals(item.getRole()))) {
            vo.setSummary("先聊几轮，我再根据真实聊天记录自动更新训练词。");
            vo.setReadyToApply(false);
            return vo;
        }

        List<TrainingMessage> transcriptWithReply = new ArrayList<>(transcript);
        transcriptWithReply.add(new TrainingMessage(ROLE_ASSISTANT, assistantReply));

        String draftPrompt;
        try {
            draftPrompt = buildDraftPrompt(transcriptWithReply, dto == null ? null : dto.getCurrentDraftPrompt());
        } catch (Exception e) {
            log.warn("Build AI training draft failed, fallback to heuristic: {}", e.getMessage());
            draftPrompt = buildHeuristicPrompt(transcriptWithReply);
        }

        List<String> highlights = collectHighlights(transcriptWithReply);
        List<String> warnings = collectWarnings(transcriptWithReply);
        List<String> missingDimensions = collectMissingDimensions(transcriptWithReply);

        vo.setDraftPrompt(draftPrompt);
        vo.setHighlights(highlights);
        vo.setWarnings(warnings);
        vo.setMissingDimensions(missingDimensions);
        vo.setSummary(missingDimensions.isEmpty()
                ? "已根据当前真实聊天记录更新训练草稿。"
                : "训练草稿已更新，但还有一些表达维度建议继续补充。");
        vo.setReadyToApply(missingDimensions.isEmpty());
        return vo;
    }

    private String buildAssistantReply(List<TrainingMessage> transcript) {
        StringBuilder prompt = new StringBuilder()
                .append("你是后台 AI 训练访谈助手。").append('\n')
                .append("只根据当前这段真实对话记录，摸清用户的语气、称呼、表达习惯、边界和技术交流方式。").append('\n')
                .append("回复要求：像正常聊天，不要问卷腔；每次先接住用户的话，再追问一个最有价值的问题；单次回复尽量简洁。").append('\n')
                .append("不要输出列表，不要直接生成训练词。").append('\n');

        if (transcript.isEmpty()) {
            prompt.append("现在请主动开场，引导用户开始聊。");
        } else {
            prompt.append("以下是当前真实聊天记录：").append('\n');
            for (TrainingMessage item : transcript) {
                prompt.append('[')
                        .append(item.getRole())
                        .append("] ")
                        .append(item.getContent())
                        .append('\n');
            }
            prompt.append("请继续这段训练对话。");
        }

        String reply = StringUtils.trimToEmpty(aiUtil.send(prompt.toString()));
        if (StringUtils.isBlank(reply)) {
            throw new ServiceException("训练对话暂时没有返回内容，请稍后再试");
        }
        return reply;
    }

    private String buildDraftPrompt(List<TrainingMessage> transcript, String currentDraftPrompt) {
        StringBuilder prompt = new StringBuilder()
                .append("你是聊天风格训练提炼器。").append('\n')
                .append("请只根据 transcript 里 user 角色的真实发言提炼风格，不要把 assistant 说过的话当作证据。").append('\n')
                .append("输出一份可以直接给 AI 使用的中文训练词，结构固定为：").append('\n')
                .append("【整体原则】").append('\n')
                .append("【聊天语气】").append('\n')
                .append("【表达习惯】").append('\n')
                .append("【任务回应方式】").append('\n')
                .append("【边界】").append('\n')
                .append("【代表性样本】").append('\n')
                .append("不要输出 JSON，不要解释。").append('\n');

        if (StringUtils.isNotBlank(currentDraftPrompt)) {
            prompt.append("当前草稿：").append('\n')
                    .append(currentDraftPrompt.trim()).append('\n');
        }

        prompt.append("transcript:").append('\n');
        for (TrainingMessage item : transcript) {
            prompt.append('[')
                    .append(item.getRole())
                    .append("] ")
                    .append(item.getContent())
                    .append('\n');
        }

        String draft = StringUtils.trimToEmpty(aiUtil.send(prompt.toString()));
        if (StringUtils.isBlank(draft)) {
            throw new ServiceException("训练草稿生成失败");
        }
        return draft;
    }

    private String buildHeuristicPrompt(List<TrainingMessage> transcript) {
        List<String> userMessages = transcript.stream()
                .filter(item -> ROLE_USER.equals(item.getRole()))
                .map(TrainingMessage::getContent)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        Set<String> toneRules = new LinkedHashSet<>();
        Set<String> expressionRules = new LinkedHashSet<>();
        Set<String> boundaryRules = new LinkedHashSet<>();

        toneRules.add("默认自然、口语化，不要客服腔。");
        toneRules.add("先接住用户情绪或问题，再给回应。");

        boolean directStyle = userMessages.stream().anyMatch(item -> item.length() <= 12);
        if (directStyle) {
            expressionRules.add("习惯短句和直接表达，不要绕弯。");
        } else {
            expressionRules.add("表达保持清楚直接，必要时再补充细节。");
        }

        boolean techMentioned = userMessages.stream().anyMatch(item ->
                item.contains("技术") || item.contains("服务器") || item.contains("部署") || item.contains("bug"));
        if (techMentioned) {
            expressionRules.add("遇到技术问题时，先给结论，再补步骤和原因。");
        }

        boundaryRules.add("没有明确关系时，不要擅自使用过于亲密的称呼。");
        boundaryRules.add("不要把助手自己的话当成用户习惯。");

        StringBuilder builder = new StringBuilder();
        builder.append("【整体原则】").append('\n')
                .append("默认先解决用户问题，再决定是否增加风格化表达。").append('\n')
                .append("只依据真实聊天记录学习，不编造经历和设定。").append('\n')
                .append('\n')
                .append("【聊天语气】").append('\n');
        toneRules.forEach(item -> builder.append(item).append('\n'));
        builder.append('\n').append("【表达习惯】").append('\n');
        expressionRules.forEach(item -> builder.append(item).append('\n'));
        builder.append('\n').append("【任务回应方式】").append('\n')
                .append("普通问题先直接回应；技术问题优先给结论，再拆步骤。").append('\n')
                .append("如果信息不足，继续追问一个最关键的问题。").append('\n');
        builder.append('\n').append("【边界】").append('\n');
        boundaryRules.forEach(item -> builder.append(item).append('\n'));
        builder.append('\n').append("【代表性样本】").append('\n');
        userMessages.stream().limit(4).forEach(item -> builder.append("- ").append(item).append('\n'));
        return builder.toString().trim();
    }

    private List<String> collectHighlights(List<TrainingMessage> transcript) {
        return transcript.stream()
                .filter(item -> ROLE_USER.equals(item.getRole()))
                .map(TrainingMessage::getContent)
                .filter(StringUtils::isNotBlank)
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<String> collectWarnings(List<TrainingMessage> transcript) {
        Set<String> warnings = new LinkedHashSet<>();
        boolean hasRelationshipWords = transcript.stream()
                .filter(item -> ROLE_USER.equals(item.getRole()))
                .map(TrainingMessage::getContent)
                .anyMatch(item -> item.contains("对象") || item.contains("喜欢") || item.contains("亲密"));
        if (hasRelationshipWords) {
            warnings.add("亲密表达只能在明确关系场景下使用，不能默认泛化。");
        }
        warnings.add("训练词只能依据真实聊天记录，不要把助手自己的话写进用户习惯。");
        return new ArrayList<>(warnings);
    }

    private List<String> collectMissingDimensions(List<TrainingMessage> transcript) {
        String merged = transcript.stream()
                .filter(item -> ROLE_USER.equals(item.getRole()))
                .map(TrainingMessage::getContent)
                .collect(Collectors.joining(" "));
        List<String> missing = new ArrayList<>();
        if (!containsAny(merged, "称呼", "叫我", "你叫")) {
            missing.add("缺少对称呼偏好的明确表达");
        }
        if (!containsAny(merged, "技术", "服务器", "部署", "bug")) {
            missing.add("缺少技术问题场景下的回复偏好");
        }
        if (!containsAny(merged, "别", "不要", "不能", "边界")) {
            missing.add("缺少边界和禁区表达");
        }
        return missing;
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = StringUtils.defaultString(text);
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<TrainingMessage> normalizeMessages(List<AiTrainingChatMessageDto> messages) {
        List<TrainingMessage> result = new ArrayList<>();
        if (messages == null || messages.isEmpty()) {
            return result;
        }
        int start = Math.max(0, messages.size() - MAX_MESSAGE_COUNT);
        for (int index = start; index < messages.size(); index++) {
            AiTrainingChatMessageDto message = messages.get(index);
            if (message == null || StringUtils.isBlank(message.getContent())) {
                continue;
            }
            String role = ROLE_ASSISTANT.equalsIgnoreCase(message.getRole()) ? ROLE_ASSISTANT : ROLE_USER;
            String content = StringUtils.normalizeSpace(message.getContent()).trim();
            if (content.length() > MAX_MESSAGE_LENGTH) {
                content = content.substring(0, MAX_MESSAGE_LENGTH);
            }
            result.add(new TrainingMessage(role, content));
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    private static class TrainingMessage {
        private String role;
        private String content;
    }
}
