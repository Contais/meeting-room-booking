package com.tulingshop.meeting.controller;

import com.tulingshop.common.result.Result;
import com.tulingshop.meeting.model.entity.MeetingRoomReservation;
import com.tulingshop.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约控制器
 */
@RestController
@RequestMapping("/meeting/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/{id}")
    public Result<MeetingRoomReservation> getReservation(@PathVariable Long id) {
        return Result.ok(reservationService.getReservationById(id));
    }

    @GetMapping("/list/{roomId}")
    public Result<List<MeetingRoomReservation>> listByRoom(@PathVariable Long roomId) {
        return Result.ok(reservationService.listByRoomId(roomId));
    }

    @PostMapping("/create")
    public Result<Void> createReservation(@RequestBody MeetingRoomReservation reservation) {
        reservationService.createReservation(reservation);
        return Result.ok();
    }
}
