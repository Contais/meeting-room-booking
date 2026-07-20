package com.tulingshop.auth.service.impl;

import com.tulingshop.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 鉴权服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public String login(String username, String password) {
        // TODO: 查询用户、校验密码、签发 JWT
        log.info("用户登录: {}", username);
        return "jwt-token-placeholder";
    }

    @Override
    public String refreshToken(String token) {
        // TODO: 校验旧 Token、签发新 Token
        log.info("刷新Token");
        return "new-jwt-token-placeholder";
    }
}
