package com.meetinghub.auth.controller;

import com.meetinghub.auth.model.dto.LoginDTO;
import com.meetinghub.auth.service.AuthService;
import com.meetinghub.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public Result<String> refreshToken(@RequestBody Map<String, String> params) {
        return Result.ok(authService.refreshToken(params.get("token")));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return Result.ok();
    }
}
