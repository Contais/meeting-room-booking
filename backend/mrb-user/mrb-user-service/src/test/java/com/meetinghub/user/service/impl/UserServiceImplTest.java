package com.meetinghub.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.crypto.digest.BCrypt;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.RoleEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.api.feign.FileFeignClient;
import com.meetinghub.user.model.dto.UserCreateDTO;
import com.meetinghub.user.model.dto.UserUpdateDTO;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.repository.DepartmentRepository;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private FileFeignClient fileFeignClient;

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
        ReflectionTestUtils.setField(userService, "baseMapper", userRepository);
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
    void should_createUser_when_validData() {
        when(userRepository.selectOne(any(), anyBoolean())).thenReturn(null);
        when(userRepository.selectCount(any())).thenReturn(0L);
        when(userRepository.insert(any(User.class))).thenReturn(1);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setPhone("13900139000");

        userService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).insert(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(RoleEnum.USER.getCode());
        assertThat(captor.getValue().getStatus()).isEqualTo(EnableStatusEnum.ENABLED.getCode());
        assertThat(BCrypt.checkpw("password123", captor.getValue().getPassword())).isTrue();
    }

    @Test
    void should_throw_when_createUserWithDuplicateUsername() {
        when(userRepository.selectOne(any(), anyBoolean())).thenReturn(mockUser);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.USER_ALREADY_EXISTS.getCode()));
    }

    @Test
    void should_throw_when_createUserWithDuplicatePhone() {
        when(userRepository.selectOne(any(), anyBoolean())).thenReturn(null);
        when(userRepository.selectCount(any())).thenReturn(1L);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setPhone("13800138000");

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS.getCode()));
    }

    @Test
    void should_throw_when_updateUserPhoneConflict() {
        when(userRepository.selectById(1L)).thenReturn(mockUser);
        when(userRepository.selectCount(any())).thenReturn(1L);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(1L);
        dto.setPhone("13900139000");

        assertThatThrownBy(() -> userService.updateUser(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS.getCode()));
    }

    @Test
    void should_toggleStatus_when_enabled() {
        when(userRepository.selectById(1L)).thenReturn(mockUser);

        userService.toggleStatus(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EnableStatusEnum.DISABLED.getCode());
    }

    @Test
    void should_throw_when_deleteAdmin() {
        mockUser.setRole(RoleEnum.ADMIN.getCode());
        when(userRepository.selectById(1L)).thenReturn(mockUser);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void should_resetPassword_withBcrypt() {
        when(userRepository.selectById(1L)).thenReturn(mockUser);

        userService.resetPassword(1L, "newpassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).updateById(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo("newpassword");
        assertThat(BCrypt.checkpw("newpassword", captor.getValue().getPassword())).isTrue();
    }

    @Test
    void should_returnUsers_when_departmentTreeHasEnabledDescendants() {
        when(departmentService.listEnabledDescendantIds(3L)).thenReturn(Set.of(3L, 4L));
        when(userRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user(1L, 3L), user(2L, 4L)));
        when(departmentRepository.selectBatchIds(any())).thenReturn(Collections.emptyList());

        List<com.meetinghub.user.model.vo.UserVO> result = userService.listContactsByDepartmentTree(3L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(com.meetinghub.user.model.vo.UserVO::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void should_returnEmpty_when_departmentTreeEmpty() {
        when(departmentService.listEnabledDescendantIds(3L)).thenReturn(Collections.emptySet());

        List<com.meetinghub.user.model.vo.UserVO> result = userService.listContactsByDepartmentTree(3L);

        assertThat(result).isEmpty();
        verify(userRepository, never()).selectList(any(LambdaQueryWrapper.class));
    }

    private User user(Long id, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDepartmentId(departmentId);
        return user;
    }

}
