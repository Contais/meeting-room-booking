package com.meetinghub.user.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileDTO implements Serializable {

    private String phone;
    private String realName;
}
