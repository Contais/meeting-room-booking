package com.meetinghub.auth.service;

import com.meetinghub.auth.model.dto.LoginVO;

/**
 * 鉴权服务接口
 */
public interface AuthService {

    /** 用户登录，返回 JWT Token */
    LoginVO login(String username, String password);

    /** 刷新 Token */
    String refreshToken(String token);

    /** 用户登出，清除 Redis Token */
    void logout(String token);
}
