package com.meetingroombook.user.service;

import com.meetingroombook.user.model.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    User getUserById(Long id);

    User getUserByUsername(String username);

    void register(String username, String password, String phone);
}
