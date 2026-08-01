package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.vo.NotificationVO;
import com.meetinghub.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内信通知控制器
 * <p>
 * 路径前缀 {@code /platform/notification/**}（用户接口）与 {@code /platform/internal/notification/**}（服务间 Feign），
 * 统一收敛至 mrb-platform 命名空间，由网关 {@code /api/platform/**} 路由分发。
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // === 用户接口 ===

    @GetMapping("/platform/notification/page")
    public Result<IPage<NotificationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        return Result.ok(notificationService.page(UserContext.getCurrentUserId(), type, isRead, page, size));
    }

    @GetMapping("/platform/notification/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.getCurrentUserId()));
    }

    @PostMapping("/platform/notification/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @PostMapping("/platform/notification/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(UserContext.getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/platform/notification/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    // === 内部接口（仅供服务间 Feign 调用，绕过网关鉴权） ===

    @PostMapping("/platform/internal/notification/send")
    public Result<Void> send(@RequestBody NotificationSendDTO dto) {
        notificationService.send(dto);
        return Result.ok();
    }

    @PostMapping("/platform/internal/notification/send-batch")
    public Result<Void> sendBatch(@RequestParam List<Long> userIds, @RequestBody NotificationSendDTO template) {
        notificationService.sendBatch(userIds, template);
        return Result.ok();
    }
}
