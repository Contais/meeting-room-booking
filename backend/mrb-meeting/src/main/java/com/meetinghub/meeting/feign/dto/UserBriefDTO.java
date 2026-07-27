package com.meetinghub.meeting.feign.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户简要信息 DTO（跨服务传输）
 * <p>
 * 解耦 mrb-user 的 UserVO：mrb-meeting 通过 Feign 拉取用户信息时统一映射为此 DTO，
 * 避免对 mrb-user 模块的强类型耦合。字段命名与 mrb-user 的 UserVO 保持一致，
 * 由 Jackson 自动完成反序列化。
 * </p>
 */
@Data
public class UserBriefDTO implements Serializable {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String realName;
    private String role;
    private Integer status;
    private Long departmentId;
    private String departmentName;
}
