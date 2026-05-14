package com.boylu.service.impl;

import com.boylu.service.MessageService;
import com.boylu.entity.SysMessage;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysMessageMapper;
import com.boylu.utils.HtmlSanitizerUtil;
import com.boylu.utils.IpUtil;
import com.boylu.utils.SensitiveUtil;
import com.boylu.utils.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_CONTENT_LENGTH = 80;
    private static final int MAX_NICKNAME_LENGTH = 30;
    private static final int MAX_BROWSER_LENGTH = 80;
    private static final String DEFAULT_NICKNAME = "\u6e38\u5ba2";
    private static final String DEFAULT_AVATAR = "/boylu-avatar.jpg";

    private final SysMessageMapper messageMapper;

    @Override
    public List<SysMessage> getMessageList() {
        return messageMapper.selectList(null);
    }

    @Override
    public Boolean add(SysMessage sysMessage) {
        if (sysMessage == null) {
            throw new ServiceException("\u7559\u8a00\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String ip = IpUtil.getIp();
        sysMessage.setIp(ip);
        sysMessage.setSource(IpUtil.getIp2region(ip));
        String sanitizedContent = HtmlSanitizerUtil.sanitizeUserRichText(sysMessage.getContent());
        if (StringUtils.isBlank(sanitizedContent)) {
            throw new ServiceException("\u7559\u8a00\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (sanitizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException("\u7559\u8a00\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc7 " + MAX_CONTENT_LENGTH + " \u4e2a\u5b57\u7b26");
        }
        sysMessage.setContent(SensitiveUtil.filter(sanitizedContent));
        sysMessage.setNickname(normalizeText(sysMessage.getNickname(), DEFAULT_NICKNAME, MAX_NICKNAME_LENGTH));
        sysMessage.setAvatar(normalizeAvatar(sysMessage.getAvatar()));
        sysMessage.setBrowser(normalizeBrowser(sysMessage.getBrowser()));
        messageMapper.insert(sysMessage);
        return true;
    }

    private String normalizeText(String value, String fallback, int maxLength) {
        String normalized = HtmlSanitizerUtil.sanitizeUserRichText(StringUtils.defaultString(value)).trim();
        if (StringUtils.isBlank(normalized)) {
            normalized = fallback;
        }
        normalized = SensitiveUtil.filter(normalized);
        return StringUtils.left(normalized, maxLength);
    }

    private String normalizeAvatar(String avatar) {
        String normalized = StringUtils.defaultString(avatar).trim();
        if (StringUtils.isBlank(normalized)) {
            return DEFAULT_AVATAR;
        }
        return StringUtils.left(normalized, 255);
    }

    private String normalizeBrowser(String browser) {
        String normalized = StringUtils.defaultString(browser).trim();
        if (StringUtils.isBlank(normalized) && IpUtil.getRequest() != null) {
            normalized = UserAgentUtil.getBrowser(IpUtil.getRequest().getHeader("User-Agent"));
        }
        return StringUtils.left(StringUtils.defaultString(normalized), MAX_BROWSER_LENGTH);
    }
}
