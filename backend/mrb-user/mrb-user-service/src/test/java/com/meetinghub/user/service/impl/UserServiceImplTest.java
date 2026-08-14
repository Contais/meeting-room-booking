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

}
