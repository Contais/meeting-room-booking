package com.meetinghub.platform.api.feign;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.MailSendDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 邮件发送远程调用客户端
 * <p>
 * 邮件域归属 mrb-platform，路径前缀 {@code /platform/internal/mail/**}。
 * 调用方需自行处理失败容错（如验证码场景降级日志输出）。
 * </p>
 */
@FeignClient(name = "mrb-platform", contextId = "mailFeignClient")
public interface MailFeignClient {

    @PostMapping("/platform/internal/mail/send")
    Result<Void> send(@RequestBody MailSendDTO dto);
}
