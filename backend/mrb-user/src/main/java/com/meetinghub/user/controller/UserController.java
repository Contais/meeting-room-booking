package com.meetinghub.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO.getUsername(), registerDTO.getPassword(), registerDTO.getPhone());
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        return Result.ok(userService.getUserDetail(Long.parseLong(userId)));
    }

    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@RequestHeader("X-User-Id") String userId,
                                      @RequestBody UserProfileDTO dto) {
        userService.updateProfile(Long.parseLong(userId), dto);
        return Result.ok();
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") String userId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(Long.parseLong(userId), dto);
        return Result.ok();
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
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
