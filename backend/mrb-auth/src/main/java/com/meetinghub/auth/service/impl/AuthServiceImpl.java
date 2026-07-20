package com.meetinghub.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.auth.feign.UserFeignClient;
import com.meetinghub.auth.service.AuthService;
import com.meetinghub.auth.util.JwtUtils;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
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

        Map<String, Object> userData = result.getData();

        // 2. 校验用户状态
        Integer status = (Integer) userData.get("status");
        if (status == null || status == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        // 3. 校验密码（BCrypt）
        String storedHash = (String) userData.get("password");
        if (storedHash == null || !BCrypt.checkpw(password, storedHash)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        // 4. 签发 JWT
        Long userId = ((Number) userData.get("id")).longValue();
        String token = jwtUtils.generateToken(userId, username);

        // 5. 存储 Token 到 Redis（用于后续校验和踢人）
        String redisKey = USER_TOKEN + userId;
        redisTemplate.opsForValue().set(redisKey, token, 24, TimeUnit.HOURS);

        log.info("用户登录成功: userId={}, username={}", userId, username);
        return token;
    }

    @Override
    public String refreshToken(String token) {
        // 1. 校验旧 Token 是否存在
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 2. 检查是否已过期
        if (jwtUtils.isTokenExpired(token)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        // 3. 解析旧 Token 信息
        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);

        // 4. 签发新 Token
        String newToken = jwtUtils.generateToken(userId, username);

        // 5. 更新 Redis
        String redisKey = USER_TOKEN + userId;
        redisTemplate.opsForValue().set(redisKey, newToken, 24, TimeUnit.HOURS);

        log.info("Token 刷新成功: userId={}", userId);
        return newToken;
    }
}
