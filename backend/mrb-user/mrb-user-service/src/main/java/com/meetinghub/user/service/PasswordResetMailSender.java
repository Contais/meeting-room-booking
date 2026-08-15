package com.meetinghub.user.service;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.feign.MailFeignClient;
import com.meetinghub.platform.api.model.dto.MailSendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 密码重置验证码邮件发送器。
 * <p>
 * 邮件发送能力由 mrb-platform 统一承载，本类仅负责组装内容并经 Feign 调用。
 * 容错策略：平台未配置 SMTP（MAIL_NOT_CONFIGURED）或 Feign 调用失败时，
 * 降级为日志输出验证码，保证本地开发/演示环境无需邮件服务器也能走通找回流程；
 * SMTP 已配置但发送失败时抛出业务异常，提示用户稍后重试。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailSender {

    private final MailFeignClient mailFeignClient;

    public void sendCode(String to, String code) {
        MailSendDTO dto = new MailSendDTO();
        dto.setTo(to);
        dto.setSubject("会议室预约系统 - 密码重置验证码");
        dto.setContent("您的验证码为：" + code + "，5 分钟内有效。若非本人操作请忽略本邮件。");
        try {
            Result<Void> result = mailFeignClient.send(dto);
            if (result.getCode() != null && result.getCode().equals(ErrorCode.MAIL_NOT_CONFIGURED.getCode())) {
                log.warn("平台邮件服务未配置，密码重置验证码仅输出日志: email={}, code={}", to, code);
                return;
            }
            if (result.getCode() == null || !result.getCode().equals(ErrorCode.SUCCESS.getCode())) {
                log.error("密码重置验证码邮件发送失败: email={}, message={}", to, result.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "验证码邮件发送失败，请稍后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("邮件服务调用失败，密码重置验证码仅输出日志: email={}, code={}", to, code, e);
            return;
        }
        log.info("密码重置验证码邮件已发送: email={}", to);
    }
}
