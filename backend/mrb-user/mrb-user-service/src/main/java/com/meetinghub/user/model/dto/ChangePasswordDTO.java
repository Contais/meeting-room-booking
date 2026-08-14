package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求 DTO
 */
@Data
public class ChangePasswordDTO implements Serializable {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度6-64个字符")
    @Schema(description = "新密码（6-64 位）")
    private String newPassword;
}
