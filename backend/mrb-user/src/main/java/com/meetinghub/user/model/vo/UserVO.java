package com.meetinghub.user.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息 DTO
 */
@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String phone;
    private String realName;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}
