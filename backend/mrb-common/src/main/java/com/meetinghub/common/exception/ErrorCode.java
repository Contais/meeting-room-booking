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
    EQUIPMENT_CODE_DUPLICATE(1020, "设备编码已存在"),

    // 参会人相关
    ATTENDEE_NOT_INVITED(1021, "该用户未被邀请"),
    ATTENDEE_ALREADY_INVITED(1022, "该用户已被邀请"),

    // 预约归属
    RESERVATION_ACCESS_DENIED(1023, "无权操作他人预约"),

    // 密码找回相关 102x
    EMAIL_NOT_BOUND(1024, "该账号未绑定邮箱，请联系管理员重置密码"),
    PASSWORD_RESET_CODE_INVALID(1025, "验证码错误或已过期"),
    PASSWORD_RESET_TOO_MANY(1026, "验证码错误次数过多，请稍后重试"),
    MAIL_NOT_CONFIGURED(1027, "邮件服务未配置"),

    // 文件相关 110x
    FILE_EMPTY(1101, "上传文件不能为空"),
    FILE_TOO_LARGE(1102, "上传文件超过大小限制"),
    FILE_TYPE_NOT_SUPPORTED(1103, "不支持的文件类型"),
    FILE_BIZ_TYPE_INVALID(1104, "无效的业务类型"),
    FILE_UPLOAD_FAILED(1105, "文件上传失败"),
    FILE_NOT_FOUND(1106, "文件不存在");

    private final Integer code;
    private final String message;
}
