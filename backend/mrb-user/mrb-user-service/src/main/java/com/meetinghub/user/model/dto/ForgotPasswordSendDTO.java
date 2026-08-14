package com.meetinghub.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送密码重置验证码请求 DTO
 */
@Data
public class ForgotPasswordSendDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    private String username;
}
