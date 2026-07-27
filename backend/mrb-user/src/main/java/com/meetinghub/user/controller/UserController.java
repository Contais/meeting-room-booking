package com.meetinghub.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.model.dto.AuthUserDTO;
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
        return Result.ok(userService.getUserDetail(id));
    }

    @GetMapping("/info/username/{username}")
    public Result<UserVO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(userService.getUserDetail(user.getId()));
    }

    @GetMapping("/internal/info/username/{username}")
    public Result<AuthUserDTO> getUserForAuth(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        AuthUserDTO dto = new AuthUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return Result.ok(dto);
    }

    /**
     * 批量查询用户名（id -> username），供跨服务回填展示名，消除 N+1 调用
     */
    @GetMapping("/internal/batch")
    public Result<java.util.Map<Long, String>> batchUsernames(@RequestParam java.util.List<Long> ids) {
        return Result.ok(userService.getUsernamesByIds(ids));
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
    public Result<Void> updateProfile(@RequestBody UserProfileDTO dto) {
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

    @RequiresRole("admin")
    @GetMapping("/admin/list")
    public Result<IPage<UserVO>> listUsers(UserPageQuery query) {
        return Result.ok(userService.listUsers(query));
    }

    @RequiresRole("admin")
    @GetMapping("/admin/detail/{id}")
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    @RequiresRole("admin")
    @PostMapping("/admin/create")
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/update")
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("admin")
    @PutMapping("/admin/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return Result.ok();
    }

    @RequiresRole("admin")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
