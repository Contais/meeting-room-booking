package com.meetinghub.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetinghub.common.enums.RoleEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.dto.UserCreateDTO;
import com.meetinghub.user.model.dto.UserDTO;
import com.meetinghub.user.model.dto.UserPageQuery;
import com.meetinghub.user.model.dto.UserUpdateDTO;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String phone) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
        }
        if (phone != null && !phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
        User existing = getUserByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (phone != null && !phone.isEmpty()) {
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setPhone(phone);
        user.setRole(RoleEnum.USER.getCode());
        user.setStatus(1);
        userRepository.insert(user);
    }

    // === 管理接口实现 ===

    @Override
    public IPage<UserDTO> listUsers(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, query.getKeyword())
                    .or()
                    .like(User::getPhone, query.getKeyword())
            );
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userRepository.selectPage(page, wrapper);
        return result.convert(this::toDTO);
    }

    @Override
    public UserDTO getUserDetail(Long id) {
        User user = getUserById(id);
        return toDTO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto) {
        if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
        }
        if (StringUtils.hasText(dto.getPhone()) && !PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }

        User existing = getUserByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(dto.getPhone())) {
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRealName(dto.getRealName());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : RoleEnum.USER.getCode());
        user.setStatus(1);
        userRepository.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = getUserById(dto.getId());

        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(user.getPhone())) {
            if (!PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
                throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
            }
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, dto.getPhone())
                            .ne(User::getId, dto.getId())
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        user.setPhone(dto.getPhone());
        user.setRealName(dto.getRealName());
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(dto.getRole());
        }
        userRepository.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        User user = getUserById(id);
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userRepository.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (RoleEnum.ADMIN.getCode().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "不允许删除管理员账号");
        }
        // TODO: 检查是否有未完成的预约（Phase 3）
        userRepository.deleteById(id);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setRealName(user.getRealName());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        return dto;
    }
}
