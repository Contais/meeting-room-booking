package com.meetinghub.user.controller;

import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.DepartmentCreateDTO;
import com.meetinghub.user.model.dto.DepartmentUpdateDTO;
import com.meetinghub.user.model.vo.DepartmentVO;
import com.meetinghub.user.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 */
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
@Tag(name = "部门", description = "部门树与部门管理")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "查询部门树")
    @GetMapping("/tree")
    public Result<List<DepartmentVO>> listTree() {
        return Result.ok(departmentService.listTree());
    }

    @Operation(summary = "查询部门扁平列表")
    @GetMapping("/list")
    public Result<List<DepartmentVO>> listFlat() {
        return Result.ok(departmentService.listFlat());
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增部门")
    @PostMapping("/admin/create")
    public Result<Void> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        departmentService.create(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑部门")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody DepartmentUpdateDTO dto) {
        departmentService.update(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除部门", description = "存在子部门或关联用户时拒绝删除")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok();
    }
}
