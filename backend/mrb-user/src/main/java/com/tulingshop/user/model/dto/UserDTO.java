package com.tulingshop.user.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息 DTO（不含密码，用于跨服务传输）
 */
@Data
public class UserDTO implements Serializable {

    private Long id;
    private String username;
    private String phone;
    private Integer status;
}
