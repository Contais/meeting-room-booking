package com.meetinghub.auth.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO implements Serializable {

    private String token;
    private Long userId;
    private String username;
    private String role;
}
