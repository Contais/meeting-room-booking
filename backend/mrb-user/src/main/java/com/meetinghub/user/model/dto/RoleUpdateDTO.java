package com.meetinghub.user.model.dto;

import lombok.Data;

/**
 * 角色更新DTO
 */
@Data
public class RoleUpdateDTO {

    private Long id;

    private String roleName;

    private String description;

    private Integer sort;
}
