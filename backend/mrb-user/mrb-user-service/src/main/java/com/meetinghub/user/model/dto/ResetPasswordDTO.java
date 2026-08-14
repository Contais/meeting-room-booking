package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 重置密码请求 DTO
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须为6-32位")
    @Schema(description = "新密码（6-32 位）")
    private String newPassword;
}
