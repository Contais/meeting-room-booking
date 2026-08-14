package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.EquipmentCreateDTO;
import com.meetinghub.meeting.model.dto.EquipmentPageQuery;
import com.meetinghub.meeting.model.dto.EquipmentUpdateDTO;
import com.meetinghub.meeting.model.vo.EquipmentVO;
import com.meetinghub.meeting.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备控制器
 */
@RestController
@RequestMapping("/meeting/equipment")
@RequiredArgsConstructor
@Tag(name = "设备", description = "设备与会议室设备分配")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // === 管理接口 ===

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "分页查询设备")
    @GetMapping("/admin/list")
    public Result<IPage<EquipmentVO>> listEquipments(EquipmentPageQuery query) {
        return Result.ok(equipmentService.listEquipments(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询设备详情")
    @GetMapping("/admin/detail/{id}")
    public Result<EquipmentVO> getEquipmentDetail(@PathVariable Long id) {
        return Result.ok(equipmentService.getEquipmentDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询启用设备列表")
    @GetMapping("/admin/active")
    public Result<List<EquipmentVO>> listActiveEquipments() {
        return Result.ok(equipmentService.listActiveEquipments());
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询某会议室的设备")
    @GetMapping("/admin/room/{roomId}")
    public Result<List<EquipmentVO>> listEquipmentsByRoom(@PathVariable Long roomId) {
        return Result.ok(equipmentService.listEquipmentsByRoomId(roomId));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增设备")
    @PostMapping("/admin/create")
    public Result<Void> createEquipment(@Valid @RequestBody EquipmentCreateDTO dto) {
        equipmentService.createEquipment(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑设备")
    @PutMapping("/admin/update")
    public Result<Void> updateEquipment(@Valid @RequestBody EquipmentUpdateDTO dto) {
        equipmentService.updateEquipment(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "启用/禁用设备")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        equipmentService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除设备")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "为设备分配会议室")
    @PutMapping("/admin/{equipmentId}/assign-rooms")
    public Result<Void> assignRooms(@PathVariable Long equipmentId,
                                    @RequestBody List<EquipmentCreateDTO.RoomEquipmentItem> rooms) {
        equipmentService.assignRooms(equipmentId, rooms);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "为会议室分配设备")
    @PutMapping("/admin/room/{roomId}/assign-equipments")
    public Result<Void> assignEquipments(@PathVariable Long roomId,
                                         @RequestBody List<Long> equipmentIds) {
        equipmentService.assignEquipments(roomId, equipmentIds);
        return Result.ok();
    }
}
