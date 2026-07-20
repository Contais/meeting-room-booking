package com.tulingshop.user.service.impl;

import com.tulingshop.common.exception.BusinessException;
import com.tulingshop.common.exception.ErrorCode;
import com.tulingshop.user.model.entity.User;
import com.tulingshop.user.repository.UserRepository;
import com.tulingshop.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
