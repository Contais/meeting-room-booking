package com.tulingshop.meeting.controller;

import com.tulingshop.common.result.Result;
import com.tulingshop.meeting.model.entity.MeetingRoom;
import com.tulingshop.meeting.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室控制器
 */
@RestController
@RequestMapping("/meeting/room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping("/{id}")
    public Result<MeetingRoom> getRoom(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getRoomById(id));
    }

    @GetMapping("/list")
    public Result<List<MeetingRoom>> listRooms() {
        return Result.ok(meetingRoomService.listRooms());
    }

    @PostMapping("/add")
    public Result<Void> addRoom(@RequestBody MeetingRoom meetingRoom) {
        meetingRoomService.addRoom(meetingRoom);
        return Result.ok();
    }
}
