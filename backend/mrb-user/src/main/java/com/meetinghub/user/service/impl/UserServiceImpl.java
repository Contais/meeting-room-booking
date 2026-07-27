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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserRepository, User> implements UserService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public User getUserById(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        return getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    @Override
    public Map<Long, String> getUsernamesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = listByIds(ids);
        return users.stream().collect(
                Collectors.toMap(User::getId, u -> u.getUsername() != null ? u.getUsername() : "")
        );
    }

    private User getActiveUserByUsername(String username) {
        return getOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode())
        );
    }

    /**
     * 校验用户名格式
     */
    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ErrorCode.USERNAME_FORMAT_ERROR);
        }
    }

    /**
     * 校验手机号格式（仅在非空时校验）
     */
    private void validatePhoneFormat(String phone) {
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PHONE_FORMAT_ERROR);
        }
    }

    /**
     * 校验用户名未被占用（新建场景）
     */
    private void checkUsernameNotExists(String username) {
        if (getActiveUserByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    /**
     * 校验手机号未被占用（排除指定用户 ID，新建传 null）
     */
    private void checkPhoneNotExists(String phone, Long excludeUserId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getDeleted, DeletedEnum.NOT_DELETED.getCode());
        if (excludeUserId != null) {
            wrapper.ne(User::getId, excludeUserId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String phone, String email) {
        validateUsername(username);
        validatePhoneFormat(phone);
        checkUsernameNotExists(username);
        checkPhoneNotExists(phone, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(RoleEnum.USER.getCode());
        user.setStatus(EnableStatusEnum.ENABLED.getCode());
        save(user);
        log.info("用户注册成功, userId={}, username={}", user.getId(), username);
    }

    @Override
    public IPage<UserVO> listUsers(UserPageQuery query) {
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        return userRepository.selectUserPage(page, query).convert(this::toVO);
    }

    @Override
    public UserVO getUserDetail(Long id) {
        return toVO(getUserById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto) {
        validateUsername(dto.getUsername());
        validatePhoneFormat(dto.getPhone());
        checkUsernameNotExists(dto.getUsername());
        checkPhoneNotExists(dto.getPhone(), null);

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRealName(dto.getRealName());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : RoleEnum.USER.getCode());
        user.setDepartmentId(dto.getDepartmentId());
        user.setStatus(EnableStatusEnum.ENABLED.getCode());
        save(user);
        log.info("管理员创建用户, userId={}, username={}", user.getId(), dto.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = getUserById(dto.getId());
        // 手机号变更时校验格式与唯一性
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(user.getPhone())) {
            validatePhoneFormat(dto.getPhone());
            checkPhoneNotExists(dto.getPhone(), dto.getId());
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRealName(dto.getRealName());
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(dto.getRole());
        }
        user.setDepartmentId(dto.getDepartmentId());
        updateById(user);
        log.info("管理员更新用户, userId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        User user = getUserById(id);
        Integer newStatus = user.getStatus().equals(EnableStatusEnum.ENABLED.getCode())
                ? EnableStatusEnum.DISABLED.getCode()
                : EnableStatusEnum.ENABLED.getCode();
        user.setStatus(newStatus);
        updateById(user);
        log.info("用户状态切换, userId={}, newStatus={}", id, newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (RoleEnum.ADMIN.getCode().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "不允许删除管理员账号");
        }
        removeById(id);
        log.info("用户删除, userId={}, username={}", id, user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserProfileDTO dto) {
        User user = getUserById(userId);
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(user.getPhone())) {
            validatePhoneFormat(dto.getPhone());
            checkPhoneNotExists(dto.getPhone(), userId);
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRealName(dto.getRealName());
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        updateById(user);
        log.info("用户更新个人资料, userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = getUserById(userId);
        if (user.getPassword() == null || !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            log.warn("修改密码失败：旧密码错误, userId={}", userId);
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "旧密码错误");
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        updateById(user);
        log.info("用户修改密码, userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.setPassword(BCrypt.hashpw(newPassword));
        updateById(user);
        log.info("管理员重置用户密码, userId={}", userId);
    }

    @Override
    public List<UserVO> listContacts(String keyword, Long departmentId) {
        List<User> users = userRepository.selectContacts(keyword, departmentId);
        return toVOList(users);
    }

    @Override
    public List<UserVO> listByIdsDetailed(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return toVOList(listByIds(ids));
    }

    /**
     * 批量转换 User -> UserVO，一次性查询部门信息消除 N+1
     */
    private List<UserVO> toVOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> deptIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = batchQueryDeptNames(deptIds);
        return users.stream().map(u -> toVO(u, deptNameMap)).collect(Collectors.toList());
    }

    /**
     * 批量查询部门名称
     */
    private Map<Long, String> batchQueryDeptNames(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Department> depts = departmentRepository.selectBatchIds(deptIds);
        return depts.stream().collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }

    /**
     * 单个 User 转 VO（详情场景使用，内部批量查询部门）
     */
    private UserVO toVO(User user) {
        Map<Long, String> deptNameMap = batchQueryDeptNames(
                user.getDepartmentId() != null ? List.of(user.getDepartmentId()) : List.of()
        );
        return toVO(user, deptNameMap);
    }

    /**
     * User 转 VO，使用预先批量查询的部门名称映射
     */
    private UserVO toVO(User user, Map<Long, String> deptNameMap) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setDepartmentId(user.getDepartmentId());
        if (user.getDepartmentId() != null) {
            vo.setDepartmentName(deptNameMap.getOrDefault(user.getDepartmentId(), ""));
        }
        return vo;
    }
}
