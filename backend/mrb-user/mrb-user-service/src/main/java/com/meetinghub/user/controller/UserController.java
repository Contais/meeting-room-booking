package com.meetinghub.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.dto.ResetPasswordDTO;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        // 普通用户仅能查自己，管理员可查任意；业务层在 service 中校验
        return Result.ok(userService.getUserDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/info/username/{username}")
    public Result<UserVO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(userService.getUserDetail(user.getId()));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO.getUsername(), registerDTO.getPassword(), registerDTO.getPhone(), registerDTO.getEmail());
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.ok(userService.getUserDetail(UserContext.getCurrentUserId()));
    }

    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileDTO dto) {
        userService.updateProfile(UserContext.getCurrentUserId(), dto);
        return Result.ok();
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.getCurrentUserId(), dto);
        return Result.ok();
    }

    /**
     * 通讯录：获取所有启用用户列表（按部门）
     */
    @GetMapping("/contacts")
    public Result<java.util.List<UserVO>> listContacts(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long departmentId) {
        return Result.ok(userService.listContacts(keyword, departmentId));
    }

    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/admin/list")
    public Result<IPage<UserVO>> listUsers(UserPageQuery query) {
        return Result.ok(userService.listUsers(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @GetMapping("/admin/detail/{id}")
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @PostMapping("/admin/create")
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/update")
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @PutMapping("/admin/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
