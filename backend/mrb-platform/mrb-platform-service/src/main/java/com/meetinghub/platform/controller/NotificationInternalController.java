package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.platform.service.NotificationService;
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
public class NotificationInternalController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public Result<Void> send(@RequestBody NotificationSendDTO dto) {
        notificationService.send(dto);
        return Result.ok();
    }

    @PostMapping("/send-batch")
    public Result<Void> sendBatch(@RequestParam List<Long> userIds, @RequestBody NotificationSendDTO template) {
        notificationService.sendBatch(userIds, template);
        return Result.ok();
    }
}
