package com.meetinghub.user.controller;

import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.MenuCreateDTO;
import com.meetinghub.user.model.dto.MenuUpdateDTO;
import com.meetinghub.user.model.vo.MenuVO;
import com.meetinghub.user.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public Result<List<MenuVO>> listTree() {
        return Result.ok(menuService.listTree());
    }

    @GetMapping("/my")
    public Result<List<MenuVO>> listByRole() {
        return Result.ok(menuService.listByRole(UserContext.getCurrentRole()));
    }

    @RequiresRole("ROLE_ADMIN")
    @PostMapping("/admin/create")
    public Result<Void> create(@Valid @RequestBody MenuCreateDTO dto) {
        menuService.create(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody MenuUpdateDTO dto) {
        menuService.update(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/role-menus")
    public Result<Void> saveRoleMenus(@RequestBody Map<String, Object> body) {
        String role = (String) body.get("role");
        @SuppressWarnings("unchecked")
        List<Long> menuIds = ((List<Number>) body.get("menuIds")).stream()
                .map(Number::longValue).toList();
        menuService.saveRoleMenus(role, menuIds);
        return Result.ok();
    }
}
