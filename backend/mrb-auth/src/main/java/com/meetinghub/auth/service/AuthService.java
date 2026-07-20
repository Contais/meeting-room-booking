package com.meetinghub.auth.service;

/**
 * 鉴权服务接口
 */
public interface AuthService {

    String login(String username, String password);

    String refreshToken(String token);

    void logout(String token);
}
