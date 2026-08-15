package com.meetinghub.user.service;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 密码重置验证码邮件发送器。
 * <p>
 * 未配置 SMTP（无 {@link JavaMailSender} Bean）时降级为日志输出，
 * 保证本地开发/演示环境无需邮件服务器也能走通找回流程。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * 发件人地址，默认取 SMTP 授权账号。
     * QQ 邮箱等服务要求 From 必须与授权账号一致，否则报 501；
     * 未配置邮件时为空，跳过设置。
     */
    @Value("${spring.mail.username:}")
    private String from;

    public void sendCode(String to, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP 未配置，密码重置验证码仅输出日志: email={}, code={}", to, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject("会议室预约系统 - 密码重置验证码");
        message.setText("您的验证码为：" + code + "，5 分钟内有效。若非本人操作请忽略本邮件。");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("密码重置验证码邮件发送失败: email={}", to, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "验证码邮件发送失败，请稍后重试");
        }
        log.info("密码重置验证码邮件已发送: email={}", to);
    }
}
