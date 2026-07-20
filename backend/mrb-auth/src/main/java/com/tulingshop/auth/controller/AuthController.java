package com.tulingshop.auth.controller;

import com.tulingshop.auth.model.dto.LoginDTO;
import com.tulingshop.auth.service.AuthService;
import com.tulingshop.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 鉴权控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.ok(authService.login(loginDTO.getUsername(), loginDTO.getPassword()));
    }

    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestParam String token) {
        return Result.ok(authService.refreshToken(token));
    }
}
