package com.meetinghub.user.service.impl;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.service.PasswordResetMailSender;
import com.meetinghub.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 密码重置服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private PasswordResetMailSender mailSender;

    @InjectMocks
    private PasswordResetServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void should_sendCode_when_userHasEmail() {
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        service.sendResetCode("testuser");

        verify(mailSender).sendCode(eq("testuser@example.com"), anyString());
        verify(valueOperations).set(startsWith("mrb:forgot-pwd:code:"), anyString(), eq(5L), any());
    }

    @Test
    void should_throw_when_sendCodeUserNotFound() {
        when(userService.getUserByUsername("nobody")).thenReturn(null);

        assertThatThrownBy(() -> service.sendResetCode("nobody"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_sendCodeWithoutEmail() {
        User noEmail = new User();
        noEmail.setId(2L);
        noEmail.setUsername("noemail");
        when(userService.getUserByUsername("noemail")).thenReturn(noEmail);

        assertThatThrownBy(() -> service.sendResetCode("noemail"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_NOT_BOUND.getCode()));
    }

    @Test
    void should_throw_when_sendCodeInCooldown() {
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.sendResetCode("testuser"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_resetPassword_when_codeValid() {
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(valueOperations.get(anyString())).thenReturn("123456");

        service.resetPassword("testuser", "123456", "newpassword");

        verify(userService).resetPassword(1L, "newpassword");
        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }

    @Test
    void should_throw_when_codeInvalid() {
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(valueOperations.get(anyString())).thenReturn("654321");
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.resetPassword("testuser", "wrong", "newpassword"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_INVALID.getCode()));
    }

    @Test
    void should_throw_when_tooManyAttempts() {
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(6L);

        assertThatThrownBy(() -> service.resetPassword("testuser", "wrong", "newpassword"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PASSWORD_RESET_TOO_MANY.getCode()));
    }
}
