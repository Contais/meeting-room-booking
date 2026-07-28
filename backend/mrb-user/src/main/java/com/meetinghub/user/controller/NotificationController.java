package com.meetinghub.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.model.dto.NotificationSendDTO;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.vo.NotificationVO;
import com.meetinghub.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信通知控制器
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // === 用户接口 ===

    @GetMapping("/user/notification/page")
    public Result<IPage<NotificationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        return Result.ok(notificationService.page(UserContext.getCurrentUserId(), type, isRead, page, size));
    }

    @GetMapping("/user/notification/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(UserContext.getCurrentUserId()));
    }

    @PostMapping("/user/notification/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @PostMapping("/user/notification/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead(UserContext.getCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/user/notification/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    // === 内部接口（仅供服务间 Feign 调用，绕过网关鉴权） ===

    @PostMapping("/user/internal/notification/send")
    public Result<Void> send(@RequestBody NotificationSendDTO dto) {
        notificationService.send(dto);
        return Result.ok();
    }

    @PostMapping("/user/internal/notification/send-batch")
    public Result<Void> sendBatch(@RequestParam List<Long> userIds, @RequestBody NotificationSendDTO template) {
        notificationService.sendBatch(userIds, template);
        return Result.ok();
    }
}
