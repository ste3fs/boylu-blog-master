package com.mojian.controller;

import cn.hutool.core.util.RandomUtil;
import com.mojian.common.RedisConstants;
import com.mojian.service.AuthService;
import com.mojian.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信公众号消息回调与登录控制器。
 */
@Slf4j
@Api(tags = "微信接口相关控制器")
@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
public class WeChatController {

    private final WxMpService wxMpService;

    private final AuthService authService;

    private final RedisUtil redisUtil;

    private final Pattern pattern = Pattern.compile("(?i)^DL\\d{4}$");

    @ApiOperation(value = "微信公众号服务器配置校验 token")
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String checkSignature(@RequestParam(name = "signature") String signature,
                                 @RequestParam(name = "timestamp") String timestamp,
                                 @RequestParam(name = "nonce") String nonce,
                                 @RequestParam(name = "echostr") String echostr) {
        log.info("公众号校验请求: signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        return "Invalid signature";
    }

    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String handleMsg(HttpServletRequest request) {
        try {
            String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            String encryptType = request.getParameter("encrypt_type");
            String timestamp = request.getParameter("timestamp");
            String nonce = request.getParameter("nonce");
            String msgSignature = request.getParameter("msg_signature");

            WxMpXmlMessage message = parseMessage(requestBody, encryptType, timestamp, nonce, msgSignature);
            String content = message.getContent();

            log.info("公众号消息: type={}, encryptType={}, from={}, content={}",
                    message.getMsgType(), encryptType, message.getFromUser(), content);

            if (!WxConsts.XmlMsgType.TEXT.equals(message.getMsgType())) {
                return "";
            }

            if ("验证码".equals(content)) {
                String code = RandomUtil.randomNumbers(4);
                String msg = MessageFormat.format("您的本次验证码:{0},该验证码5分钟内有效。", code);
                redisUtil.set(RedisConstants.CAPTCHA_CODE_KEY + code, code, RedisConstants.FIVE_MINUTES_EXPIRE, TimeUnit.SECONDS);
                return returnMsg(msg, message, encryptType);
            }

            if (content != null && content.toLowerCase().contains("dl")) {
                Matcher matcher = pattern.matcher(content.trim());
                if (!matcher.matches()) {
                    return returnMsg("验证码不正确或已过期", message, encryptType);
                }
                String msg = authService.wechatLogin(message);
                return returnMsg(msg, message, encryptType);
            }

            return "";
        } catch (Exception e) {
            log.error("微信公众号消息处理失败", e);
            return "";
        }
    }

    private WxMpXmlMessage parseMessage(String requestBody,
                                        String encryptType,
                                        String timestamp,
                                        String nonce,
                                        String msgSignature) {
        if ("aes".equalsIgnoreCase(encryptType)) {
            return WxMpXmlMessage.fromEncryptedXml(
                    requestBody,
                    wxMpService.getWxMpConfigStorage(),
                    timestamp,
                    nonce,
                    msgSignature
            );
        }
        return WxMpXmlMessage.fromXml(requestBody);
    }

    private String returnMsg(String msg, WxMpXmlMessage message, String encryptType) {
        WxMpXmlOutTextMessage outMessage = WxMpXmlOutTextMessage.TEXT()
                .content(msg)
                .fromUser(message.getToUser())
                .toUser(message.getFromUser())
                .build();
        if ("aes".equalsIgnoreCase(encryptType)) {
            return outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
        }
        return outMessage.toXml();
    }
}
