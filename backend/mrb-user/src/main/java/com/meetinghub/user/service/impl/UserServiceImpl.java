package com.meetinghub.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.DeletedEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.RoleEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.model.entity.Department;
import com.meetinghub.user.repository.DepartmentRepository;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserRepository, User> implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

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

    @Override
    public Map<Long, String> getUsernamesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<User> users = userRepository.selectBatchIds(ids);
        return users.stream().collect(
                Collectors.toMap(User::getId, u -> u.getUsername() != null ? u.getUsername() : "")
        );
    }

    private User getActiveUserByUsername(String username) {
        return userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode())
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
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone).eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode())
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
        user.setStatus(EnableStatusEnum.ENABLED.getCode());
        userRepository.insert(user);
    }

    @Override
    public IPage<UserVO> listUsers(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, query.getKeyword())
                    .or().like(User::getRealName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(User::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(User::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCreateTimeStart())) {
            wrapper.ge(User::getCreateTime, query.getCreateTimeStart());
        }
        if (StringUtils.hasText(query.getCreateTimeEnd())) {
            wrapper.le(User::getCreateTime, query.getCreateTimeEnd());
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
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode())
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
        user.setDepartmentId(dto.getDepartmentId());
        user.setStatus(EnableStatusEnum.ENABLED.getCode());
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
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode()).ne(User::getId, dto.getId())
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
        user.setDepartmentId(dto.getDepartmentId());
        userRepository.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        User user = getUserById(id);
        Integer newStatus = user.getStatus().equals(EnableStatusEnum.ENABLED.getCode())
                ? EnableStatusEnum.DISABLED.getCode()
                : EnableStatusEnum.ENABLED.getCode();
        user.setStatus(newStatus);
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
                    new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()).eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode()).ne(User::getId, userId)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(BCrypt.hashpw(newPassword));
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
        vo.setDepartmentId(user.getDepartmentId());
        if (user.getDepartmentId() != null) {
            Department dept = departmentRepository.selectById(user.getDepartmentId());
            if (dept != null) {
                vo.setDepartmentName(dept.getName());
            }
        }
        return vo;
    }
}
