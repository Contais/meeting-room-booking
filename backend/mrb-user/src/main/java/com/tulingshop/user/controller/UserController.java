package com.tulingshop.user.controller;

import com.tulingshop.common.result.Result;
import com.tulingshop.user.model.dto.RegisterDTO;
import com.tulingshop.user.model.dto.UserDTO;
import com.tulingshop.user.model.entity.User;
import com.tulingshop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    @GetMapping("/info/username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        return Result.ok(dto);
    }

    /**
     * 内部接口：供鉴权服务调用，返回含密码哈希的用户信息
     */
    @GetMapping("/internal/info/username/{username}")
    public Result<Map<String, Object>> getUserForAuth(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("password", user.getPassword());
        map.put("status", user.getStatus());
        return Result.ok(map);
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
}
