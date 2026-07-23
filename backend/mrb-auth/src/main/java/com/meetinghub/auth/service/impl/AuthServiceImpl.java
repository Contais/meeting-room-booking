package com.meetinghub.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.auth.feign.UserFeignClient;
import com.meetinghub.auth.model.dto.LoginVO;
import com.meetinghub.auth.service.AuthService;
import com.meetinghub.auth.util.JwtUtils;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.model.dto.AuthUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.meetinghub.common.constant.RedisKeyConstant.USER_TOKEN;

@Slf4j
/** 鉴权服务实现 */
@Service
@RequiredArgsConstructor
/** 鉴权服务实现 */
public class AuthServiceImpl implements AuthService {

    private final UserFeignClient userFeignClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginVO login(String username, String password) {
        var result = userFeignClient.getUserForAuth(username);
        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        AuthUserDTO user = result.getData();

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        String redisKey = USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, 24, TimeUnit.HOURS);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRole(user.getRole());

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return loginVO;
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);

        String newToken = jwtUtils.generateToken(userId, username, role);

        String redisKey = USER_TOKEN + userId;
        redisTemplate.opsForValue().set(redisKey, newToken, 24, TimeUnit.HOURS);

        log.info("Token 刷新成功: userId={}", userId);
        return newToken;
    }

    @Override
    public void logout(String token) {
        if (!jwtUtils.validateToken(token)) {
            return;
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        String redisKey = USER_TOKEN + userId;
        redisTemplate.delete(redisKey);
        log.info("用户登出: userId={}", userId);
    }
}
