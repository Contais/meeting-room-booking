package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员编辑用户 DTO
 */
@Data
public class UserUpdateDTO implements Serializable {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String phone;

    private String realName;

    private String role;
}
