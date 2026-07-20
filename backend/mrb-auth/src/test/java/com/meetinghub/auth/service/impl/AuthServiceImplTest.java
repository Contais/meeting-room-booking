package com.meetinghub.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.auth.feign.UserFeignClient;
import com.meetinghub.auth.model.dto.LoginVO;
import com.meetinghub.auth.util.JwtUtils;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthUserDTO mockAuthUser;
    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        mockAuthUser = new AuthUserDTO();
        mockAuthUser.setId(1L);
        mockAuthUser.setUsername("testuser");
        mockAuthUser.setPassword(BCrypt.hashpw("password123"));
        mockAuthUser.setRole("user");
        mockAuthUser.setStatus(1);
    }

    @Test
    void should_returnLoginVO_when_loginWithCorrectCredentials() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockAuthUser));
        when(jwtUtils.generateToken(1L, "testuser", "user")).thenReturn(VALID_TOKEN);

        LoginVO result = authService.login("testuser", "password123");

        assertThat(result.getToken()).isEqualTo(VALID_TOKEN);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRole()).isEqualTo("user");
        verify(valueOperations).set(eq("mrb:user:token:1"), eq(VALID_TOKEN), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void should_throwException_when_loginWithWrongPassword() {
        when(userFeignClient.getUserForAuth("testuser")).thenReturn(Result.ok(mockAuthUser));

        assertThatThrownBy(() -> authService.login("testuser", "wrongpassword"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getMessage())
                        .isEqualTo("用户名或密码错误"));
    }

    @Test
    void should_throwException_when_loginWithNonexistentUser() {
        when(userFeignClient.getUserForAuth("nouser")).thenReturn(Result.ok(null));

        assertThatThrownBy(() -> authService.login("nouser", "pass"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    void should_throwException_when_loginWithDisabledUser() {
        AuthUserDTO disabledUser = new AuthUserDTO();
        disabledUser.setId(2L);
        disabledUser.setUsername("disabled");
        disabledUser.setPassword(BCrypt.hashpw("pass"));
        disabledUser.setRole("user");
        disabledUser.setStatus(0);

        when(userFeignClient.getUserForAuth("disabled")).thenReturn(Result.ok(disabledUser));

        assertThatThrownBy(() -> authService.login("disabled", "pass"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getMessage())
                        .isEqualTo("账号已被禁用"));
    }

    @Test
    void should_returnNewToken_when_refreshValidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(1L);
        when(jwtUtils.getUsernameFromToken(VALID_TOKEN)).thenReturn("testuser");
        when(jwtUtils.getRoleFromToken(VALID_TOKEN)).thenReturn("user");
        when(jwtUtils.generateToken(1L, "testuser", "user")).thenReturn("new.jwt.token");

        String newToken = authService.refreshToken(VALID_TOKEN);

        assertThat(newToken).isEqualTo("new.jwt.token");
    }

    @Test
    void should_throwException_when_refreshInvalidToken() {
        when(jwtUtils.validateToken("invalid")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("invalid"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID.getCode()));
    }

    @Test
    void should_deleteRedisKey_when_logout() {
        when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(1L);

        authService.logout(VALID_TOKEN);

        verify(redisTemplate).delete("mrb:user:token:1");
    }

    @Test
    void should_doNothing_when_logoutWithInvalidToken() {
        when(jwtUtils.validateToken("invalid")).thenReturn(false);

        authService.logout("invalid");

        verify(redisTemplate, never()).delete(anyString());
    }
}
