package com.meetinghub.meeting.controller;

import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.vo.AttendeeVO;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约参会人控制器
 * <p>
 * 提供参会人邀请（按用户/按部门）、查询、移除接口，仅预约创建者可操作。
 * </p>
 */
@RestController
@RequestMapping("/meeting/reservation/attendee")
@RequiredArgsConstructor
public class ReservationAttendeeController {

    private final ReservationAttendeeService attendeeService;

    /**
     * 邀请参会人（按用户 ID 列表），返回实际新增数量
     */
    @PostMapping("/{reservationId}/invite")
    public Result<Integer> inviteAttendees(@PathVariable Long reservationId,
                                            @RequestBody InviteRequest req) {
        int count = attendeeService.inviteAttendees(reservationId, UserContext.getCurrentUserId(), req.getUserIds());
        return Result.ok(count);
    }

    /**
     * 按部门邀请参会人
     */
    @PostMapping("/{reservationId}/invite-department")
    public Result<Integer> inviteDepartment(@PathVariable Long reservationId,
                                             @RequestBody InviteDepartmentRequest req) {
        int count = attendeeService.inviteDepartment(reservationId, UserContext.getCurrentUserId(), req.getDepartmentId());
        return Result.ok(count);
    }

    /**
     * 查询预约的参会人列表
     */
    @GetMapping("/{reservationId}/list")
    public Result<List<AttendeeVO>> listAttendees(@PathVariable Long reservationId) {
        return Result.ok(attendeeService.listAttendees(reservationId));
    }

    /**
     * 移除参会人
     */
    @DeleteMapping("/{reservationId}/{userId}")
    public Result<Void> removeAttendee(@PathVariable Long reservationId,
                                        @PathVariable Long userId) {
        attendeeService.removeAttendee(reservationId, userId, UserContext.getCurrentUserId());
        return Result.ok();
    }

    /**
     * 参会人响应邀请（更新自己的参会状态）
     *
     * @param reservationId 预约ID
     * @param req           状态请求体: status=1 已接受, status=2 已拒绝
     */
    @PutMapping("/{reservationId}/respond")
    public Result<Void> respondInvitation(@PathVariable Long reservationId,
                                          @RequestBody RespondRequest req) {
        attendeeService.updateAttendeeStatus(reservationId, UserContext.getCurrentUserId(), req.getStatus());
        return Result.ok();
    }

    /** 邀请参会人请求体 */
    @lombok.Data
    public static class InviteRequest {
        private List<Long> userIds;
    }

    /** 按部门邀请请求体 */
    @lombok.Data
    public static class InviteDepartmentRequest {
        private Long departmentId;
    }

    /** 参会人响应请求体 */
    @lombok.Data
    public static class RespondRequest {
        /** 1-已接受, 2-已拒绝 */
        private Integer status;
    }
}
