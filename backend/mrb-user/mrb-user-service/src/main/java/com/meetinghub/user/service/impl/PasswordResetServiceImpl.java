package com.meetinghub.user.service.impl;

import com.meetinghub.common.constant.RedisKeyConstant;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.service.PasswordResetMailSender;
import com.meetinghub.user.service.PasswordResetService;
import com.meetinghub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 密码重置服务实现。
 * <p>
 * 验证码存 Redis（key 前缀 mrb:），5 分钟有效；发送有 60 秒冷却；
 * 校验失败最多 5 次，超过后需等待冷却窗口重新获取验证码。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final long CODE_TTL_MINUTES = 5;
    private static final long COOLDOWN_SECONDS = 60;
    private static final long MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordResetMailSender mailSender;

    @Override
    public void sendResetCode(String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_BOUND);
        }

        String cooldownKey = RedisKeyConstant.FORGOT_PWD_COOLDOWN + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(
                RedisKeyConstant.FORGOT_PWD_CODE + username,
                code,
                CODE_TTL_MINUTES,
                TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
        redisTemplate.delete(RedisKeyConstant.FORGOT_PWD_ATTEMPTS + username);

        mailSender.sendCode(user.getEmail(), code);
    }

    @Override
    public void resetPassword(String username, String code, String newPassword) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户不存在");
        }

        String codeKey = RedisKeyConstant.FORGOT_PWD_CODE + username;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            Long attempts = redisTemplate.opsForValue().increment(
                    RedisKeyConstant.FORGOT_PWD_ATTEMPTS + username);
            if (attempts != null && attempts == 1L) {
                redisTemplate.expire(
                        RedisKeyConstant.FORGOT_PWD_ATTEMPTS + username,
                        CODE_TTL_MINUTES,
                        TimeUnit.MINUTES);
            }
            if (attempts != null && attempts > MAX_ATTEMPTS) {
                throw new BusinessException(ErrorCode.PASSWORD_RESET_TOO_MANY);
            }
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        userService.resetPassword(user.getId(), newPassword);
        redisTemplate.delete(codeKey);
        redisTemplate.delete(RedisKeyConstant.FORGOT_PWD_COOLDOWN + username);
        redisTemplate.delete(RedisKeyConstant.FORGOT_PWD_ATTEMPTS + username);
        log.info("用户通过验证码重置密码成功, username={}", username);
    }
}
