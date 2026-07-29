package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 参会人查阅状态枚举
 */
@Getter
@AllArgsConstructor
public enum AttendeeStatusEnum {

    PENDING(0, "待查阅"),
    ACCEPTED(1, "已查阅"),
    DECLINED(2, "已拒绝");

    private final Integer code;
    private final String desc;
}
