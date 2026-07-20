package com.meetinghub.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.auth.feign.UserFeignClient;
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

/**
 * 鉴权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserFeignClient userFeignClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String login(String username, String password) {
        // 1. 通过 Feign 调用用户服务查询用户
        var result = userFeignClient.getUserForAuth(username);
        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        AuthUserDTO user = result.getData();

        // 2. 校验用户状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        // 3. 校验密码（BCrypt）
        if (user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        // 4. 签发 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 存储 Token 到 Redis（用于后续校验和踢人）
        String redisKey = USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, 24, TimeUnit.HOURS);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return token;
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);

        String newToken = jwtUtils.generateToken(userId, username);

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
