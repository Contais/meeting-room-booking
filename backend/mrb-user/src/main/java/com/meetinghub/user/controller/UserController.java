package com.meetinghub.user.controller;

import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import com.meetinghub.user.model.dto.RegisterDTO;
import com.meetinghub.user.model.dto.UserDTO;
import com.meetinghub.user.model.entity.User;
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
    public Result<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.ok(toDTO(user));
    }

    @GetMapping("/info/username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        return Result.ok(toDTO(user));
    }

    /**
     * 内部接口：供鉴权服务调用
     */
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
        dto.setStatus(user.getStatus());
        return Result.ok(dto);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getPhone()
        );
        return Result.ok();
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
