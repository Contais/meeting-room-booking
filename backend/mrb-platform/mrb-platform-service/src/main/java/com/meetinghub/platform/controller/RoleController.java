package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.dto.RoleCreateDTO;
import com.meetinghub.platform.model.dto.RoleMenuAssignDTO;
import com.meetinghub.platform.model.dto.RoleUpdateDTO;
import com.meetinghub.platform.model.vo.RoleVO;
import com.meetinghub.platform.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/page")
    public Result<IPage<RoleVO>> pageRoles(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.pageRoles(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<RoleVO>> listAllRoles() {
        return Result.ok(roleService.listAllRoles());
    }

    @GetMapping("/{id}")
    public Result<RoleVO> getRoleDetail(@PathVariable Long id) {
        return Result.ok(roleService.getRoleDetail(id));
    }

    @PostMapping("/create")
    public Result<Void> createRole(@RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> updateRole(@RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(dto);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleRoleStatus(@PathVariable Long id) {
        roleService.toggleRoleStatus(id);
        return Result.ok();
    }

    @PutMapping("/assign-menus")
    public Result<Void> assignMenus(@RequestBody RoleMenuAssignDTO dto) {
        roleService.assignMenus(dto);
        return Result.ok();
    }

    @GetMapping("/{id}/menu-ids")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long id) {
        return Result.ok(roleService.getRoleMenuIds(id));
    }
}
