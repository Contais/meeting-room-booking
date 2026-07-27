package com.meetinghub.meeting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.dto.EquipmentCreateDTO;
import com.meetinghub.meeting.model.dto.EquipmentPageQuery;
import com.meetinghub.meeting.model.dto.EquipmentUpdateDTO;
import com.meetinghub.meeting.model.vo.EquipmentVO;
import com.meetinghub.meeting.service.EquipmentService;
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
public class EquipmentController {

    private final EquipmentService equipmentService;

    // === 管理接口 ===

    @RequiresRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<EquipmentVO>> listEquipments(EquipmentPageQuery query) {
        return Result.ok(equipmentService.listEquipments(query));
    }

    @RequiresRole("admin")
    @GetMapping("/admin/detail/{id}")
    public Result<EquipmentVO> getEquipmentDetail(@PathVariable Long id) {
        return Result.ok(equipmentService.getEquipmentDetail(id));
    }

    @RequiresRole("admin")
    @GetMapping("/admin/active")
    public Result<List<EquipmentVO>> listActiveEquipments() {
        return Result.ok(equipmentService.listActiveEquipments());
    }

    @RequiresRole("admin")
    @GetMapping("/admin/room/{roomId}")
    public Result<List<EquipmentVO>> listEquipmentsByRoom(@PathVariable Long roomId) {
        return Result.ok(equipmentService.listEquipmentsByRoomId(roomId));
    }

    @RequiresRole("admin")
    @PostMapping("/admin/create")
    public Result<Void> createEquipment(@Valid @RequestBody EquipmentCreateDTO dto) {
        equipmentService.createEquipment(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/update")
    public Result<Void> updateEquipment(@Valid @RequestBody EquipmentUpdateDTO dto) {
        equipmentService.updateEquipment(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        equipmentService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("admin")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/{equipmentId}/assign-rooms")
    public Result<Void> assignRooms(@PathVariable Long equipmentId,
                                    @RequestBody List<EquipmentCreateDTO.RoomEquipmentItem> rooms) {
        equipmentService.assignRooms(equipmentId, rooms);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/room/{roomId}/assign-equipments")
    public Result<Void> assignEquipments(@PathVariable Long roomId,
                                         @RequestBody List<Long> equipmentIds) {
        equipmentService.assignEquipments(roomId, equipmentIds);
        return Result.ok();
    }
}
