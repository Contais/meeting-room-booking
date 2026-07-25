package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单可见性枚举
 */
@Getter
@AllArgsConstructor
public enum VisibleEnum {

    HIDDEN(0, "隐藏"),
    VISIBLE(1, "显示");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举实例
     */
    public static VisibleEnum fromCode(Integer code) {
        for (VisibleEnum visible : values()) {
            if (visible.code.equals(code)) {
                return visible;
            }
        }
        return VISIBLE;
    }
}
