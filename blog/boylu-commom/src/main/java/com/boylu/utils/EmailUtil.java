package com.boylu.utils;

import com.boylu.common.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author: boylu
 * @date: 2024/12/28
 * @description: 邮箱工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtil {

    private static final String EMAIL_LOGO = "data:image/png;base64,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";


    @Value("${mail.smtp.email}")
    private String fromEmail;

    @Value("${mail.smtp.password}")
    private String password;

    @Value("${mail.smtp.port}")
    private int port;

    @Value("${mail.smtp.host}")
    private String host;

    @Value("${mail.smtp.ssl-enable:false}")
    private boolean sslEnable;

    @Value("${mail.smtp.starttls-enable:false}")
    private boolean starttlsEnable;

    @Value("${mail.smtp.debug:false}")
    private boolean debug;

    @Value("${PUBLIC_BASE_URL:http://localhost:3000}")
    private String publicBaseUrl;

    private final RedisUtil redisUtil;

    private final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();



    public void getJavaMailSenderImpl(){
        javaMailSender.setHost(host);
        javaMailSender.setUsername(fromEmail);
        javaMailSender.setPassword(password);
        javaMailSender.setPort(port);
        javaMailSender.setDefaultEncoding("UTF-8");
        Properties p = new Properties();
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.debug", String.valueOf(debug));
        p.setProperty("mail.smtp.ssl.enable", String.valueOf(sslEnable));
        p.setProperty("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        javaMailSender.setJavaMailProperties(p);
    }

    /**
     * 发送验证码
     * @param email
     * @throws MessagingException
     */
    public void sendCode(String email) throws MessagingException {

        this.getJavaMailSenderImpl();

        int code = (int) ((Math.random() * 9 + 1) * 100000);
        String content = "<html>\n" +
                "\t<body><div id=\"contentDiv\" onmouseover=\"getTop().stopPropagation(event);\" onclick=\"getTop().preSwapLink(event, 'html', 'ZC0004_vDfNJayMtMUuKGIAzzsWvc8');\" style=\"position:relative;font-size:14px;height:auto;padding:15px 15px 10px 15px;z-index:1;zoom:1;line-height:1.7;\" class=\"body\">\n" +
                "  <div id=\"qm_con_body\">\n" +
                "    <div id=\"mailContentContainer\" class=\"qmbox qm_con_body_content qqmail_webmail_only\" style=\"opacity: 1;\">\n" +
                "      <style type=\"text/css\">\n" +
                "        .qmbox h1,.qmbox \t\t\th2,.qmbox \t\t\th3 {\t\t\t\tcolor: #00785a;\t\t\t}\t\t\t.qmbox p {\t\t\t\tpadding: 0;\t\t\t\tmargin: 0;\t\t\t\tcolor: #333;\t\t\t\tfont-size: 16px;\t\t\t}\t\t\t.qmbox hr {\t\t\t\tbackground-color: #d9d9d9;\t\t\t\tborder: none;\t\t\t\theight: 1px;\t\t\t}\t\t\t.qmbox .eo-link {\t\t\t\tcolor: #0576b9;\t\t\t\ttext-decoration: none;\t\t\t\tcursor: pointer;\t\t\t}\t\t\t.qmbox .eo-link:hover {\t\t\t\tcolor: #3498db;\t\t\t}\t\t\t.qmbox .eo-link:hover {\t\t\t\ttext-decoration: underline;\t\t\t}\t\t\t.qmbox .eo-p-link {\t\t\t\tdisplay: block;\t\t\t\tmargin-top: 20px;\t\t\t\tcolor: #009cff;\t\t\t\ttext-decoration: underline;\t\t\t}\t\t\t.qmbox .p-intro {\t\t\t\tpadding: 30px;\t\t\t}\t\t\t.qmbox .p-code {\t\t\t\tpadding: 0 30px 0 30px;\t\t\t}\t\t\t.qmbox .p-news {\t\t\t\tpadding: 0px 30px 30px 30px;\t\t\t}\n" +
                "      </style>\n" +
                "      <div style=\"max-width:800px;padding-bottom:10px;margin:20px auto 0 auto;\">\n" +
                "        <table cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #fff;border-collapse: collapse; border:1px solid #e5e5e5;box-shadow: 0 10px 15px rgba(0, 0, 0, 0.05);text-align: left;width: 100%;font-size: 14px;border-spacing: 0;\">\n" +
                "          <tbody>\n" +
                "            <tr style=\"background-color: #f8f8f8;\">\n" +
                "              <td>\n" +
                "                <img style=\"padding: 15px 0 15px 30px;width:50px\" src=\"" + EMAIL_LOGO + "\" />" +
                "                <span>boylu博客 </span>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td class=\"p-intro\">\n" +
                "                <h1 style=\"font-size: 26px; font-weight: bold;\">验证您的邮箱地址</h1>\n" +
                "                <p style=\"line-height:1.75em;\">感谢您使用 boylu博客. </p>\n" +
                "                <p style=\"line-height:1.75em;\">以下是您的邮箱验证码，请将它输入到  <span style=\"color:#409eff;\">boylu博客</span> 的邮箱验证码输入框中:</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td class=\"p-code\">\n" +
                "                <p style=\"color: #253858;text-align:center;line-height:1.75em;background-color: #f2f2f2;min-width: 200px;margin: 0 auto;font-size: 28px;border-radius: 5px;border: 1px solid #d9d9d9;font-weight: bold;\">"+code+"</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td class=\"p-intro\">\n" +
                "                <p style=\"line-height:1.75em;\">这一封邮件包括一些您的私密信息，请不要回复或转发它，以免带来不必要的信息泄露风险。 </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td class=\"p-intro\">\n" +
                "                <hr>\n" +
                "                <p style=\"text-align: center;line-height:1.75em;\">boylu - <a href='http://localhost:3000' style='text-decoration: none;color:#409eff'>boylu博客</a></p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </tbody>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "      <style type=\"text/css\">\n" +
                "        .qmbox style, .qmbox script, .qmbox head, .qmbox link, .qmbox meta {display: none !important;}\n" +
                "      </style>\n" +
                "    </div>\n" +
                "  </div><!-- -->\n" +
                "  <style>\n" +
                "    #mailContentContainer .txt {height:auto;}\n" +
                "  </style>\n" +
                "</div></body>\n" +
                "</html>\n";

        // 创建邮件消息
        this.send(email, content);
        log.info("邮箱验证码发送成功,邮箱:{},验证码:{}",email,code);

        redisUtil.set(RedisConstants.CAPTCHA_CODE_KEY + email, code +"");
        redisUtil.expire(RedisConstants.CAPTCHA_CODE_KEY + email, RedisConstants.FIVE_MINUTES_EXPIRE, TimeUnit.SECONDS);
    }

    private void send(String email, String template) throws MessagingException {

        //创建一个MINE消息
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mineHelper = new MimeMessageHelper(mimeMessage, true);
        // 设置邮件主题
        mineHelper.setSubject("您有一封来自 boylu博客 的回执！");
        // 设置邮件发送者
        mineHelper.setFrom(Objects.requireNonNull(javaMailSender.getUsername()));
        // 设置邮件接收者，可以有多个接收者，中间用逗号隔开
        mineHelper.setTo(email);
        // 设置邮件发送日期
        mineHelper.setSentDate(DateUtil.getNowDate());
        // 设置邮件的正文
        mineHelper.setText(template,true);
        // 发送邮件
        javaMailSender.send(mimeMessage);
    }


}
