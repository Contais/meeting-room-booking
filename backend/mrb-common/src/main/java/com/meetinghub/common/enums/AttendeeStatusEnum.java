package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 参会人状态枚举
 */
@Getter
@AllArgsConstructor
public enum AttendeeStatusEnum {

    PENDING(0, "待响应"),
    ACCEPTED(1, "已接受"),
    DECLINED(2, "已拒绝");

    private final Integer code;
    private final String desc;
}
