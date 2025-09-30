package com.hanserwei.hannote.auth.utils;

import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.hannote.auth.enums.ResponseCodeEnum;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
public class MailHelper {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String username;


    public boolean sendMail(String verificationCode, String email) {
        Context context = new Context();
        context.setVariable("verifyCode", Arrays.asList(verificationCode.split("")));

        String process;
        try {
            // 确保这里的 templateEngine 能够正确处理 context
            process = templateEngine.process("EmailVerificationCode.html", context);
        } catch (Exception e) {
            // 处理模板渲染失败的异常
            throw new ApiException(ResponseCodeEnum.TEMPLATE_RENDER_ERROR);
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setSubject("【Han-note】验证码");
            helper.setFrom(username);
            helper.setTo(email);
            helper.setSentDate(new Date());
            helper.setText(process, true);

            mailSender.send(mimeMessage); // 可能会抛出 MailException (RuntimeException)
            log.info("邮件发送成功！");
        } catch (MessagingException | org.springframework.mail.MailException e) {
            // 捕获 MimeMessageHelper 配置异常 和 mailSender 发送异常
            log.error("邮件发送失败：{}", e.getMessage());
            throw new ApiException(ResponseCodeEnum.MAIL_SEND_ERROR);
        }
        return true;
    }

}
