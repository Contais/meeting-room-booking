package com.meetingroombook.auth.controller;

import com.meetingroombook.auth.model.dto.LoginDTO;
import com.meetingroombook.auth.service.AuthService;
import com.meetingroombook.common.result.Result;
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
