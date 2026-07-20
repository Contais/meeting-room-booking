package com.meetinghub.common.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 鉴权用用户 DTO（含密码哈希，仅服务内部传输）
 */
@Data
public class AuthUserDTO implements Serializable {

    private Long id;
    private String username;
    private String password;
    private Integer status;
}
