package com.meetinghub.user.controller;

import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.DepartmentCreateDTO;
import com.meetinghub.user.model.dto.DepartmentUpdateDTO;
import com.meetinghub.user.model.vo.DepartmentVO;
import com.meetinghub.user.service.DepartmentService;
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
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/tree")
    public Result<List<DepartmentVO>> listTree() {
        return Result.ok(departmentService.listTree());
    }

    @GetMapping("/list")
    public Result<List<DepartmentVO>> listFlat() {
        return Result.ok(departmentService.listFlat());
    }

    @RequiresRole("ROLE_ADMIN")
    @PostMapping("/admin/create")
    public Result<Void> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        departmentService.create(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody DepartmentUpdateDTO dto) {
        departmentService.update(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok();
    }
}
