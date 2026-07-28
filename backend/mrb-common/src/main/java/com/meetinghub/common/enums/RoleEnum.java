package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    ADMIN("ROLE_ADMIN", "超级管理员"),
    USER("ROLE_USER", "普通用户");

    private final String code;
    private final String desc;

    public static RoleEnum fromCode(String code) {
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return USER;
    }
}
