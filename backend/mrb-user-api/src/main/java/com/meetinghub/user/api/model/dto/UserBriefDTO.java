package com.meetinghub.user.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户简要信息 DTO（跨服务传输）
 * <p>
 * 解耦 mrb-user 的 UserVO：消费方通过 Feign 拉取用户信息时统一映射为此 DTO，
 * 避免对 mrb-user 模块的强类型耦合。字段命名与 mrb-user 的 UserVO 保持一致，
 * 由 Jackson 自动完成反序列化。
 * </p>
 * <p>
 * 使用 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 容忍远端扩展字段，
 * 防止 mrb-user 的 UserVO 新增字段后导致 Feign 反序列化失败。
 * </p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
