package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 启用/禁用状态枚举
 */
@Getter
@AllArgsConstructor
public enum EnableStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举实例
     */
    public static EnableStatusEnum fromCode(Integer code) {
        for (EnableStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ENABLED;
    }
}
