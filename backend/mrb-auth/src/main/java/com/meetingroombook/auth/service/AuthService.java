package com.meetingroombook.auth.service;

/**
 * 鉴权服务接口
 */
public interface AuthService {

    /**
     * 用户登录，返回 JWT Token
     */
    String login(String username, String password);

    /**
     * 刷新 Token
     */
    String refreshToken(String token);
}
