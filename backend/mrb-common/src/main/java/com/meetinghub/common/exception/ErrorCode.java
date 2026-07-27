package com.meetinghub.common.exception;

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

    // 用户相关 100x
    USER_NOT_FOUND(1005, "用户不存在"),
    USER_ALREADY_EXISTS(1006, "用户已存在"),
    USERNAME_FORMAT_ERROR(1009, "用户名格式不正确，需2-32位字母数字下划线"),
    PHONE_FORMAT_ERROR(1010, "手机号格式不正确"),
    PHONE_ALREADY_EXISTS(1011, "手机号已注册"),

    // 鉴权相关
    AUTH_TOKEN_INVALID(1007, "Token无效或已过期"),
    AUTH_TOKEN_EXPIRED(1008, "Token已过期"),

    // 会议室相关
    MEETING_ROOM_NOT_FOUND(1001, "会议室不存在"),
    MEETING_ROOM_DISABLED(1002, "会议室已禁用"),
    RESERVATION_CONFLICT(1003, "时段已被预约"),
    RESERVATION_NOT_FOUND(1004, "预约记录不存在"),

    // 部门相关
    DEPARTMENT_NOT_FOUND(1012, "部门不存在"),
    DEPARTMENT_NAME_DUPLICATE(1013, "部门名称已存在"),
    DEPARTMENT_HAS_CHILDREN(1014, "存在子部门，不允许删除"),
    DEPARTMENT_HAS_USERS(1015, "部门下有用户，不允许删除"),
    DEPARTMENT_CIRCULAR(1016, "不能将部门移动到其子部门下"),

    // 菜单相关
    MENU_NOT_FOUND(1017, "菜单不存在"),
    MENU_HAS_CHILDREN(1018, "存在子菜单，不允许删除"),

    // 设备相关
    EQUIPMENT_NOT_FOUND(1019, "设备不存在"),
    EQUIPMENT_CODE_DUPLICATE(1020, "设备编码已存在");

    private final Integer code;
    private final String message;
}
