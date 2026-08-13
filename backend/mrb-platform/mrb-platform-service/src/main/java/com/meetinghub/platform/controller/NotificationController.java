package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.vo.NotificationVO;
import com.meetinghub.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 站内信通知控制器（用户接口）
 * <p>
 * 路径前缀 {@code /platform/notification/**}，由网关 {@code /api/platform/**} 路由分发。
 * 服务间 Feign 内部接口见 {@link NotificationInternalController}。
 * </p>
 */
@RestController
@RequestMapping("/platform/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/page")
    public Result<IPage<NotificationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        return Result.ok(notificationService.page(UserContext.getCurrentUserId(), type, isRead, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.getCurrentUserId()));
    }

    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(UserContext.getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }
}
