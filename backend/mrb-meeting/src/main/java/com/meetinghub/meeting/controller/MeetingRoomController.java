package com.meetinghub.meeting.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.service.MeetingRoomService;
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
