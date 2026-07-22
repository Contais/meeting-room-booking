package com.meetinghub.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetinghub.common.enums.RoleEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

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
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    private User getActiveUserByUsername(String username) {
        return userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
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
        if (getActiveUserByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(phone)) {
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone).eq(User::getDeleted, 0)
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

    @Override
    public IPage<UserVO> listUsers(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, query.getKeyword())
                    .or().like(User::getPhone, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userRepository.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public UserVO getUserDetail(Long id) {
        return toVO(getUserById(id));
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
        if (getActiveUserByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(dto.getPhone())) {
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, 0)
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
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, 0).ne(User::getId, dto.getId())
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
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserProfileDTO dto) {
        User user = getUserById(userId);
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(user.getPhone())) {
            if (!PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
                throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
            }
            Long count = userRepository.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, 0).ne(User::getId, userId)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }
        user.setPhone(dto.getPhone());
        user.setRealName(dto.getRealName());
        userRepository.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = getUserById(userId);
        if (user.getPassword() == null || !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "旧密码错误");
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userRepository.updateById(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
