package com.meetinghub.auth.service;

import com.meetinghub.auth.model.dto.LoginVO;

public interface AuthService {

    LoginVO login(String username, String password);

    String refreshToken(String token);

    void logout(String token);
}
