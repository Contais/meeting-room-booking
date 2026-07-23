package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;
import com.meetinghub.meeting.service.MeetingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meeting/room")
@RequiredArgsConstructor
/** 会议室控制器 */
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    // === 公开接口 ===

    @GetMapping("/list")
    public Result<List<MeetingRoomVO>> listActiveRooms() {
        return Result.ok(meetingRoomService.listActiveRooms());
    }

    @GetMapping("/{id}")
    public Result<MeetingRoomVO> getRoom(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getRoomDetail(id));
    }

    // === 管理接口 ===

    @RequiresRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<MeetingRoomVO>> listRooms(RoomPageQuery query) {
        return Result.ok(meetingRoomService.listRooms(query));
    }

    @RequiresRole("admin")
    @GetMapping("/admin/detail/{id}")
    public Result<MeetingRoomVO> getRoomDetail(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getRoomDetail(id));
    }

    @RequiresRole("admin")
    @PostMapping("/admin/create")
    public Result<Void> createRoom(@Valid @RequestBody RoomCreateDTO dto) {
        meetingRoomService.createRoom(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/update")
    public Result<Void> updateRoom(@Valid @RequestBody RoomUpdateDTO dto) {
        meetingRoomService.updateRoom(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        meetingRoomService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("admin")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        meetingRoomService.deleteRoom(id);
        return Result.ok();
    }
}
