package com.tulingshop.auth.controller;

import com.tulingshop.common.result.Result;
import com.tulingshop.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<String> login(String username, String password) {
        return Result.ok(authService.login(username, password));
    }

    @PostMapping("/refresh")
    public Result<String> refreshToken(String token) {
        return Result.ok(authService.refreshToken(token));
    }
}
