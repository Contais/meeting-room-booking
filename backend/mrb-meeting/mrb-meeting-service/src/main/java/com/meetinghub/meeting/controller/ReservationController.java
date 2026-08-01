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
public class ReservationController {

    private final ReservationService reservationService;

    // === 用户接口 ===

    @PostMapping("/create")
    public Result<String> createReservation(@Valid @RequestBody ReservationCreateDTO dto) {
        String reservationCode = reservationService.createReservation(UserContext.getCurrentUserId(), dto);
        return Result.ok(reservationCode);
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(UserContext.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<IPage<ReservationVO>> listMyReservations(ReservationPageQuery query) {
        return Result.ok(reservationService.listMyReservations(UserContext.getCurrentUserId(), query));
    }

    @GetMapping("/my-meetings")
    public Result<IPage<ReservationVO>> listMyMeetings(ReservationPageQuery query) {
        return Result.ok(reservationService.listMyMeetings(UserContext.getCurrentUserId(), query));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    public Result<List<ReservationVO>> listByRoomAndDate(@PathVariable Long roomId,
                                                         @PathVariable String date) {
        return Result.ok(reservationService.listByRoomAndDate(roomId, date));
    }

    @GetMapping("/detail/{id}")
    public Result<ReservationVO> getMyReservationDetail(@PathVariable Long id) {
        return Result.ok(reservationService.getMyReservationDetail(UserContext.getCurrentUserId(), id));
    }

    // === 日程视图 ===

    @GetMapping("/schedule")
    public Result<ScheduleVO> getSchedule(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(reservationService.getSchedule(date, startDate, endDate));
    }

    // === 管理接口 ===

    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/admin/list")
    public Result<IPage<ReservationVO>> listAllReservations(ReservationPageQuery query) {
        return Result.ok(reservationService.listAllReservations(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/approve/{id}")
    public Result<Void> approveReservation(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/reject/{id}")
    public Result<Void> rejectReservation(@PathVariable Long id, @RequestBody(required = false) RejectDTO dto) {
        String reason = dto != null ? dto.getReason() : null;
        reservationService.rejectReservation(id, reason);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/admin/detail/{id}")
    public Result<ReservationVO> getReservationDetail(@PathVariable Long id) {
        return Result.ok(reservationService.getReservationDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDeleteReservation(@PathVariable Long id) {
        reservationService.adminDeleteReservation(id);
        return Result.ok();
    }
}
