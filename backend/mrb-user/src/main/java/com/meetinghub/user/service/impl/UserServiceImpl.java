package com.meetinghub.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 校验用户名格式
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
        }

        // 校验手机号格式
        if (phone != null && !phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }

        // 校验用户名唯一性
        User existing = getUserByUsername(username);
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 校验手机号唯一性
        if (phone != null && !phone.isEmpty()) {
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, phone)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setPhone(phone);
        user.setStatus(1);
        userRepository.insert(user);
    }
}
