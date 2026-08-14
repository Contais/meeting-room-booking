package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.dto.RoleCreateDTO;
import com.meetinghub.platform.model.dto.RoleMenuAssignDTO;
import com.meetinghub.platform.model.dto.RoleUpdateDTO;
import com.meetinghub.platform.model.vo.RoleVO;
import com.meetinghub.platform.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/admin/role")
@RequiredArgsConstructor
@Tag(name = "角色", description = "角色与菜单权限分配")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public Result<IPage<RoleVO>> pageRoles(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.pageRoles(pageNum, pageSize, keyword));
    }

    @Operation(summary = "查询全部角色")
    @GetMapping("/list")
    public Result<List<RoleVO>> listAllRoles() {
        return Result.ok(roleService.listAllRoles());
    }

    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleDetail(@PathVariable Long id) {
        return Result.ok(roleService.getRoleDetail(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/create")
    public Result<Void> createRole(@RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/update")
    public Result<Void> updateRole(@RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(dto);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用角色")
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleRoleStatus(@PathVariable Long id) {
        roleService.toggleRoleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "为角色分配菜单")
    @PutMapping("/assign-menus")
    public Result<Void> assignMenus(@RequestBody RoleMenuAssignDTO dto) {
        roleService.assignMenus(dto);
        return Result.ok();
    }

    @Operation(summary = "查询角色菜单 ID 列表")
    @GetMapping("/{id}/menu-ids")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long id) {
        return Result.ok(roleService.getRoleMenuIds(id));
    }
}
