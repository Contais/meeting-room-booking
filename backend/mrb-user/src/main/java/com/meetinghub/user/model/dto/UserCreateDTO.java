package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员创建用户 DTO
 */
@Data
public class UserCreateDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度2-32个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度6-64个字符")
    private String password;

    private String phone;

    private String realName;

    private String role;
}
