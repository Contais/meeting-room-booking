package com.meetinghub.platform.controller;

import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.dto.MenuCreateDTO;
import com.meetinghub.platform.model.dto.MenuUpdateDTO;
import com.meetinghub.platform.model.vo.MenuVO;
import com.meetinghub.platform.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/menu")
@RequiredArgsConstructor
@Tag(name = "菜单", description = "菜单树与管理")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "查询菜单树")
    @GetMapping("/tree")
    public Result<List<MenuVO>> listTree() {
        return Result.ok(menuService.listTree());
    }

    @Operation(summary = "查询当前角色可见菜单")
    @GetMapping("/my")
    public Result<List<MenuVO>> listByRole() {
        return Result.ok(menuService.listByRole(UserContext.getCurrentRole()));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增菜单")
    @PostMapping("/admin/create")
    public Result<Void> create(@Valid @RequestBody MenuCreateDTO dto) {
        menuService.create(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑菜单")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody MenuUpdateDTO dto) {
        menuService.update(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除菜单", description = "存在子菜单时拒绝删除")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
