package com.meetinghub.platform.service.impl;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.api.model.dto.MailSendDTO;
import com.meetinghub.platform.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件发送服务实现
 * <p>
 * 未配置 SMTP（无 {@link JavaMailSender} Bean）时抛出 {@link ErrorCode#MAIL_NOT_CONFIGURED}，
 * 由调用方决定降级策略（如验证码场景降级日志输出）。
 * 发件人默认取 SMTP 授权账号：QQ 邮箱等服务要求 From 必须与授权账号一致，否则报 501。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * 发件人地址，默认取 SMTP 授权账号；未配置邮件时为空。
     */
    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public void send(MailSendDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "收件人邮箱不能为空");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(dto.getTo());
        message.setSubject(dto.getSubject());
        message.setText(dto.getContent());
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("邮件发送失败: to={}, subject={}", dto.getTo(), dto.getSubject(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "邮件发送失败");
        }
        log.info("邮件已发送: to={}, subject={}", dto.getTo(), dto.getSubject());
    }
}
