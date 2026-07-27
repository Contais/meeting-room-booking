package com.meetinghub.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.user.model.dto.*;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;

import java.util.Collection;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据ID查询用户
     */
    User getUserById(Long id);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 批量查询用户名（id -> username），用于跨服务回填展示名
     */
    Map<Long, String> getUsernamesByIds(Collection<Long> ids);

    /**
     * 用户注册
     */
    void register(String username, String password, String phone);

    /**
     * 分页查询用户列表（管理端）
     */
    IPage<UserVO> listUsers(UserPageQuery query);

    /**
     * 查询用户详情
     */
    UserVO getUserDetail(Long id);

    /**
     * 新增用户（管理端）
     */
    void createUser(UserCreateDTO dto);

    /**
     * 编辑用户（管理端）
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 启用/禁用用户
     */
    void toggleStatus(Long id);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long id);

    /**
     * 编辑个人信息
     */
    void updateProfile(Long userId, UserProfileDTO dto);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /**
     * 重置用户密码（管理端）
     */
    void resetPassword(Long userId, String newPassword);
}
