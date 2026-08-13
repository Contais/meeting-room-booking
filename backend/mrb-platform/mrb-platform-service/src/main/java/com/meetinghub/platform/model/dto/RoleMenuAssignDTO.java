package com.meetinghub.platform.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 角色菜单权限分配DTO
 */
@Data
public class RoleMenuAssignDTO {

    private Long roleId;

    private List<Long> menuIds;
}
