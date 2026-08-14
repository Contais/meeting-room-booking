package com.meetinghub.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.service.PasswordResetService;
import com.meetinghub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/uc/user")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户与个人中心")
public class UserController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "查询用户详情", description = "普通用户仅能查询自己，管理员可查询任意用户")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        // 普通用户仅能查自己，管理员可查任意；业务层在 service 中校验
        return Result.ok(userService.getUserDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "按用户名查询用户")
    @GetMapping("/info/username/{username}")
    public Result<UserVO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(userService.getUserDetail(user.getId()));
    }

    @Operation(summary = "查询当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.ok(userService.getUserDetail(UserContext.getCurrentUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileDTO dto) {
        userService.updateProfile(UserContext.getCurrentUserId(), dto);
        return Result.ok();
    }

    @Operation(summary = "修改个人密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.getCurrentUserId(), dto);
        return Result.ok();
    }

    @Operation(summary = "发送密码重置验证码", description = "验证码发送到账号绑定邮箱")
    @PostMapping("/forgot-password/send-code")
    public Result<Void> sendPasswordResetCode(@Valid @RequestBody ForgotPasswordSendDTO dto) {
        passwordResetService.sendResetCode(dto.getUsername());
        return Result.ok();
    }

    @Operation(summary = "通过验证码重置密码")
    @PostMapping("/forgot-password/reset")
    public Result<Void> resetPasswordByCode(@Valid @RequestBody ForgotPasswordResetDTO dto) {
        passwordResetService.resetPassword(dto.getUsername(), dto.getCode(), dto.getNewPassword());
        return Result.ok();
    }

    /**
     * 通讯录：获取所有启用用户列表（按部门）
     */
    @Operation(summary = "查询通讯录")
    @GetMapping("/contacts")
    public Result<java.util.List<UserVO>> listContacts(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long departmentId) {
        return Result.ok(userService.listContacts(keyword, departmentId));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/admin/list")
    public Result<IPage<UserVO>> listUsers(UserPageQuery query) {
        return Result.ok(userService.listUsers(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "查询用户详情")
    @GetMapping("/admin/detail/{id}")
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetail(id));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增用户")
    @PostMapping("/admin/create")
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑用户")
    @PutMapping("/admin/update")
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "启用/禁用用户")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "重置用户密码")
    @PutMapping("/admin/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除用户")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
