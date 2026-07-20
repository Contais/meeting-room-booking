package com.tulingshop.user.service;

import com.tulingshop.user.model.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    User getUserById(Long id);
}
