package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;
import com.meetinghub.meeting.service.MeetingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室控制器
 */
@RestController
@RequestMapping("/meeting/room")
@RequiredArgsConstructor
@Tag(name = "会议室", description = "会议室浏览与管理")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    // === 公开接口 ===

    @Operation(summary = "查询启用会议室列表")
    @GetMapping("/list")
    public Result<List<MeetingRoomVO>> listActiveRooms() {
        return Result.ok(meetingRoomService.listActiveRooms());
    }

    @Operation(summary = "查询会议室详情")
    @GetMapping("/{id}")
    public Result<MeetingRoomVO> getRoom(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getRoomDetail(id));
    }

    // === 管理接口 ===

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "分页查询会议室")
    @GetMapping("/admin/list")
    public Result<IPage<MeetingRoomVO>> listRooms(RoomPageQuery query) {
        return Result.ok(meetingRoomService.listRooms(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询会议室详情")
    @GetMapping("/admin/detail/{id}")
    public Result<MeetingRoomVO> getRoomDetail(@PathVariable Long id) {
        return Result.ok(meetingRoomService.getRoomDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增会议室")
    @PostMapping("/admin/create")
    public Result<Void> createRoom(@Valid @RequestBody RoomCreateDTO dto) {
        meetingRoomService.createRoom(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑会议室")
    @PutMapping("/admin/update")
    public Result<Void> updateRoom(@Valid @RequestBody RoomUpdateDTO dto) {
        meetingRoomService.updateRoom(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "启用/禁用会议室")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        meetingRoomService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除会议室")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        meetingRoomService.deleteRoom(id);
        return Result.ok();
    }
}
