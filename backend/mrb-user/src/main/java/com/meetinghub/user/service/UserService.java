package com.meetinghub.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.user.model.dto.UserCreateDTO;
import com.meetinghub.user.model.dto.UserDTO;
import com.meetinghub.user.model.dto.UserPageQuery;
import com.meetinghub.user.model.dto.UserUpdateDTO;
import com.meetinghub.user.model.entity.User;

public interface UserService {

    User getUserById(Long id);

    User getUserByUsername(String username);

    void register(String username, String password, String phone);

    IPage<UserDTO> listUsers(UserPageQuery query);

    UserDTO getUserDetail(Long id);

    void createUser(UserCreateDTO dto);

    void updateUser(UserUpdateDTO dto);

    void toggleStatus(Long id);

    void deleteUser(Long id);
}
