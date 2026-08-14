package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员编辑用户 DTO
 */
@Data
public class UserUpdateDTO implements Serializable {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色：ROLE_ADMIN / ROLE_USER")
    private String role;

    /**
     * 所属部门ID
     */
    @Schema(description = "所属部门ID")
    private Long departmentId;
}
