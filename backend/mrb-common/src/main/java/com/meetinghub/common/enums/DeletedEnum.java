package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 删除标记枚举
 */
@Getter
@AllArgsConstructor
public enum DeletedEnum {

    NOT_DELETED(0, "未删除"),
    DELETED(1, "已删除");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举实例
     */
    public static DeletedEnum fromCode(Integer code) {
        for (DeletedEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return NOT_DELETED;
    }
}
