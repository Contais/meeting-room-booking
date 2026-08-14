package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.platform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信通知内部接口控制器（服务间 Feign）
 * <p>
 * 路径前缀 {@code /platform/internal/notification/**}，仅供服务间 Feign 调用，不经过网关。
 * </p>
 */
@RestController
@RequestMapping("/platform/internal/notification")
@RequiredArgsConstructor
@Tag(name = "通知内部接口", description = "服务间 Feign 调用，不经过网关")
public class NotificationInternalController {

    private final NotificationService notificationService;

    @Operation(summary = "发送单条通知")
    @PostMapping("/send")
    public Result<Void> send(@RequestBody NotificationSendDTO dto) {
        notificationService.send(dto);
        return Result.ok();
    }

    @Operation(summary = "批量发送通知")
    @PostMapping("/send-batch")
    public Result<Void> sendBatch(@RequestParam List<Long> userIds, @RequestBody NotificationSendDTO template) {
        notificationService.sendBatch(userIds, template);
        return Result.ok();
    }
}
