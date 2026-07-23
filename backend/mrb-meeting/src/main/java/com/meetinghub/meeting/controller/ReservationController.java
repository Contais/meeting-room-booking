package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.vo.ReservationVO;
import com.meetinghub.meeting.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meeting/reservation")
@RequiredArgsConstructor
/** 预约控制器 */
public class ReservationController {

    private final ReservationService reservationService;

    // === 用户接口 ===

    @PostMapping("/create")
    public Result<Void> createReservation(@RequestHeader("X-User-Id") String userId,
                                          @Valid @RequestBody ReservationCreateDTO dto) {
        reservationService.createReservation(Long.parseLong(userId), dto);
        return Result.ok();
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancelReservation(@RequestHeader("X-User-Id") String userId,
                                          @PathVariable Long id) {
        reservationService.cancelReservation(Long.parseLong(userId), id);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<IPage<ReservationVO>> listMyReservations(
            @RequestHeader("X-User-Id") String userId,
            ReservationPageQuery query) {
        return Result.ok(reservationService.listMyReservations(Long.parseLong(userId), query));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    public Result<List<ReservationVO>> listByRoomAndDate(@PathVariable Long roomId,
                                                         @PathVariable String date) {
        return Result.ok(reservationService.listByRoomAndDate(roomId, date));
    }

    // === 管理接口 ===

    @RequiresRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<ReservationVO>> listAllReservations(ReservationPageQuery query) {
        return Result.ok(reservationService.listAllReservations(query));
    }

    @RequiresRole("admin")
    @PutMapping("/admin/approve/{id}")
    public Result<Void> approveReservation(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/reject/{id}")
    public Result<Void> rejectReservation(@PathVariable Long id) {
        reservationService.rejectReservation(id);
        return Result.ok();
    }
}
