package com.meetinghub.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通过验证码重置密码请求 DTO
 */
@Data
public class ForgotPasswordResetDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度6-64个字符")
    @Schema(description = "新密码（6-64 位）")
    private String newPassword;
}
