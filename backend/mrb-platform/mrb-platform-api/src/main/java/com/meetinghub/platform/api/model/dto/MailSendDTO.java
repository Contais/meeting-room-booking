package com.meetinghub.platform.api.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件发送 DTO（跨服务契约）
 * <p>
 * 通用三字段设计，支撑密码重置验证码等任意邮件场景，
 * 由 mrb-platform 统一承载 SMTP 发送能力。
 * </p>
 */
@Data
public class MailSendDTO implements Serializable {

    /** 收件人邮箱 */
    private String to;

    /** 邮件主题 */
    private String subject;

    /** 邮件正文（纯文本） */
    private String content;
}
