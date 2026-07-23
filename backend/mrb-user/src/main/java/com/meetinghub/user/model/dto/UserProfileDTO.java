package com.meetinghub.user.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 个人信息编辑请求 DTO
 */
@Data
public class UserProfileDTO implements Serializable {

    /** 手机号 */
    private String phone;

    /** 真实姓名 */
    private String realName;
}
