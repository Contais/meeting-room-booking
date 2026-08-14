package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.RejectDTO;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.vo.ReservationVO;
import com.meetinghub.meeting.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.meetinghub.meeting.model.vo.ScheduleVO;
import java.util.List;

/**
 * 预约控制器
 */
@RestController
@RequestMapping("/meeting/reservation")
@RequiredArgsConstructor
@Tag(name = "预约", description = "会议室预约与审批")
public class ReservationController {

    private final ReservationService reservationService;

    // === 用户接口 ===

    @Operation(summary = "创建预约")
    @PostMapping("/create")
    public Result<String> createReservation(@Valid @RequestBody ReservationCreateDTO dto) {
        String reservationCode = reservationService.createReservation(UserContext.getCurrentUserId(), dto);
        return Result.ok(reservationCode);
    }

    @Operation(summary = "取消预约")
    @PutMapping("/cancel/{id}")
    public Result<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "删除预约", description = "仅可删除已取消或已拒绝的预约")
    @DeleteMapping("/{id}")
    public Result<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "分页查询我的预约")
    @GetMapping("/my")
    public Result<IPage<ReservationVO>> listMyReservations(ReservationPageQuery query) {
        return Result.ok(reservationService.listMyReservations(UserContext.getCurrentUserId(), query));
    }

    @Operation(summary = "分页查询我作为参会人的会议")
    @GetMapping("/my-meetings")
    public Result<IPage<ReservationVO>> listMyMeetings(ReservationPageQuery query) {
        return Result.ok(reservationService.listMyMeetings(UserContext.getCurrentUserId(), query));
    }

    @Operation(summary = "查询我的日历")
    @GetMapping("/my-calendar")
    public Result<List<ReservationVO>> listMyCalendar(ReservationPageQuery query) {
        return Result.ok(reservationService.listMyCalendar(UserContext.getCurrentUserId(), query));
    }

    @Operation(summary = "查询某会议室某天的预约")
    @GetMapping("/room/{roomId}/date/{date}")
    public Result<List<ReservationVO>> listByRoomAndDate(@PathVariable Long roomId,
                                                         @PathVariable String date) {
        return Result.ok(reservationService.listByRoomAndDate(roomId, date));
    }

    @Operation(summary = "查询预约详情")
    @GetMapping("/detail/{id}")
    public Result<ReservationVO> getMyReservationDetail(@PathVariable Long id) {
        return Result.ok(reservationService.getMyReservationDetail(UserContext.getCurrentUserId(), id));
    }

    // === 日程视图 ===

    @Operation(summary = "查询日程视图")
    @GetMapping("/schedule")
    public Result<ScheduleVO> getSchedule(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(reservationService.getSchedule(date, startDate, endDate));
    }

    // === 管理接口 ===

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "分页查询全部预约")
    @GetMapping("/admin/list")
    public Result<IPage<ReservationVO>> listAllReservations(ReservationPageQuery query) {
        return Result.ok(reservationService.listAllReservations(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "审批通过预约")
    @PutMapping("/admin/approve/{id}")
    public Result<Void> approveReservation(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "审批拒绝预约")
    @PutMapping("/admin/reject/{id}")
    public Result<Void> rejectReservation(@PathVariable Long id, @RequestBody(required = false) RejectDTO dto) {
        String reason = dto != null ? dto.getReason() : null;
        reservationService.rejectReservation(id, reason);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询预约详情")
    @GetMapping("/admin/detail/{id}")
    public Result<ReservationVO> getReservationDetail(@PathVariable Long id) {
        return Result.ok(reservationService.getReservationDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除预约")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDeleteReservation(@PathVariable Long id) {
        reservationService.adminDeleteReservation(id);
        return Result.ok();
    }
}
