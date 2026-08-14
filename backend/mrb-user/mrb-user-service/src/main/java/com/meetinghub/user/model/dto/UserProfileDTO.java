package com.meetinghub.user.model.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 个人信息编辑请求 DTO
 */
@Data
public class UserProfileDTO implements Serializable {

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 头像URL
     */
    @Schema(description = "头像 objectKey")
    private String avatar;
}
