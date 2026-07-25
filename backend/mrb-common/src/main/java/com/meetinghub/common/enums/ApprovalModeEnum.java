package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批模式枚举
 */
@Getter
@AllArgsConstructor
public enum ApprovalModeEnum {

    FREE_APPROVAL(0, "免审批"),
    NEED_APPROVAL(1, "需审批");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举实例
     */
    public static ApprovalModeEnum fromCode(Integer code) {
        for (ApprovalModeEnum mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return FREE_APPROVAL;
    }
}
