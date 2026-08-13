package com.meetinghub.platform.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色VO
 */
@Data
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer isSystem;

    private Integer sort;

    private LocalDateTime createTime;

    private List<Long> menuIds;
}
