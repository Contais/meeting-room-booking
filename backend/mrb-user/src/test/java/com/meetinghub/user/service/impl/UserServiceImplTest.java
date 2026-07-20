package com.meetinghub.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("$2a$10$hash");
        mockUser.setPhone("13800138000");
        mockUser.setStatus(1);
    }

    @Test
    void should_returnUser_when_userExists() {
        when(userRepository.selectById(1L)).thenReturn(mockUser);

        User result = userService.getUserById(1L);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPhone()).isEqualTo("13800138000");
    }

    @Test
    void should_throwException_when_userNotFound() {
        when(userRepository.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    void should_returnUser_when_queryByUsername() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        User result = userService.getUserByUsername("testuser");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void should_returnNull_when_usernameNotExists() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        User result = userService.getUserByUsername("nonexist");

        assertThat(result).isNull();
    }

    @Test
    void should_insertUser_when_registerWithValidData() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userRepository.insert(any(User.class))).thenReturn(1);

        userService.register("newuser", "password123", "13900139000");

        verify(userRepository).insert(any(User.class));
    }

    @Test
    void should_throwException_when_registerWithDuplicateUsername() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        assertThatThrownBy(() -> userService.register("testuser", "pass", "13900139000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.USER_ALREADY_EXISTS.getCode()));
    }

    @Test
    void should_throwException_when_registerWithInvalidUsername() {
        assertThatThrownBy(() -> userService.register("a", "pass", "13900139000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.USERNAME_FORMAT_ERROR.getCode()));
    }

    @Test
    void should_throwException_when_registerWithInvalidPhone() {
        assertThatThrownBy(() -> userService.register("validuser", "pass", "123"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PHONE_FORMAT_ERROR.getCode()));
    }

    @Test
    void should_throwException_when_registerWithDuplicatePhone() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> userService.register("newuser", "pass", "13800138000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS.getCode()));
    }

    @Test
    void should_allowRegister_when_phoneIsNull() {
        when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userRepository.insert(any(User.class))).thenReturn(1);

        userService.register("newuser", "pass", null);

        verify(userRepository).insert(any(User.class));
    }
}
