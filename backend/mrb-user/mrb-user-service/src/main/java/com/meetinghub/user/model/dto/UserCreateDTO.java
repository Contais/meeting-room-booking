package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员创建用户 DTO
 */
@Data
public class UserCreateDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度2-32个字符")
    @Schema(description = "用户名（2-32 位字母数字下划线）")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度6-64个字符")
    @Schema(description = "密码（6-64 位）")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色：ROLE_ADMIN / ROLE_USER，默认 ROLE_USER")
    private String role;

    /**
     * 所属部门ID
     */
    @Schema(description = "所属部门ID")
    private Long departmentId;
}
