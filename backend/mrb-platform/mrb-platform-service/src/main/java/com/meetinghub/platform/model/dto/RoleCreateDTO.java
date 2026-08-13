package com.meetinghub.platform.model.dto;

import lombok.Data;

/**
 * 角色创建DTO
 */
@Data
public class RoleCreateDTO {

    private String roleCode;

    private String roleName;

    private String description;

    private Integer sort;
}
