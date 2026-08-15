package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.MailSendDTO;
import com.meetinghub.platform.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件发送内部接口控制器（服务间 Feign）
 * <p>
 * 路径前缀 {@code /platform/internal/mail/**}，仅供服务间 Feign 调用，不经过网关。
 * </p>
 */
@RestController
@RequestMapping("/platform/internal/mail")
@RequiredArgsConstructor
@Tag(name = "邮件内部接口", description = "服务间 Feign 调用，不经过网关")
public class MailInternalController {

    private final MailService mailService;

    @Operation(summary = "发送纯文本邮件")
    @PostMapping("/send")
    public Result<Void> send(@RequestBody MailSendDTO dto) {
        mailService.send(dto);
        return Result.ok();
    }
}
