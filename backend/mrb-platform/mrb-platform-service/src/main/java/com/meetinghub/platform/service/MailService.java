package com.meetinghub.platform.service;

import com.meetinghub.platform.api.model.dto.MailSendDTO;

/**
 * 邮件发送服务
 * <p>
 * 邮件域统一由 mrb-platform 承载，供各业务服务经 Feign 调用。
 * </p>
 */
public interface MailService {

    /**
     * 发送纯文本邮件
     *
     * @param dto 收件人、主题、正文
     * @throws com.meetinghub.common.exception.BusinessException 邮件服务未配置（MAIL_NOT_CONFIGURED）或发送失败
     */
    void send(MailSendDTO dto);
}
