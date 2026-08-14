package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.vo.NotificationVO;
import com.meetinghub.platform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "通知", description = "站内信通知")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "分页查询通知")
    @GetMapping("/page")
    public Result<IPage<NotificationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        return Result.ok(notificationService.page(UserContext.getCurrentUserId(), type, isRead, page, size));
    }

    @Operation(summary = "查询未读数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.getCurrentUserId()));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(UserContext.getCurrentUserId());
        return Result.ok();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }
}
