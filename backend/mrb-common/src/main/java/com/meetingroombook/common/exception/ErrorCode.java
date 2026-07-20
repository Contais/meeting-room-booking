package com.meetingroombook.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码 1xxx
    MEETING_ROOM_NOT_FOUND(1001, "会议室不存在"),
    MEETING_ROOM_DISABLED(1002, "会议室已禁用"),
    RESERVATION_CONFLICT(1003, "时段已被预约"),
    RESERVATION_NOT_FOUND(1004, "预约记录不存在"),
    USER_NOT_FOUND(1005, "用户不存在"),
    USER_ALREADY_EXISTS(1006, "用户已存在"),
    AUTH_TOKEN_INVALID(1007, "Token无效或已过期"),
    AUTH_TOKEN_EXPIRED(1008, "Token已过期");

    private final Integer code;
    private final String message;
}
